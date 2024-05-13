package com.familyplanner.location.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.common.schema.ObserverDbSchema
import com.familyplanner.common.schema.TaskDbSchema
import com.familyplanner.location.data.TaskLocationDto
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationNotificationSender(val context: Context) {
    private val tasks: HashMap<String, TaskLocationDto> = hashMapOf()
    private val firestore = Firebase.firestore
    private var listener: Job? = null
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private val RADIUS = 6371e3
    private val scope = CoroutineScope(Dispatchers.IO)
    private val lock = Any()

    fun startUpdates() {
        scope.launch {
            firestore.collection(ObserverDbSchema.OBSERVER_TABLE).whereEqualTo(ObserverDbSchema.USER_ID, FamilyPlanner.userId)
                .snapshots().collect {
                    val taskRadius = hashMapOf<String, Double>()
                    for (doc in it.documents) {
                        doc.getDouble(ObserverDbSchema.RADIUS)?.let {
                            taskRadius[doc[ObserverDbSchema.TASK_ID].toString()] = it
                        }
                    }
                    if (taskRadius.isNotEmpty()) {
                        launch {
                            listener?.cancel()
                            listener = launch {
                                firestore.collection(TaskDbSchema.TASK_TABLE)
                                    .whereIn(FieldPath.documentId(), taskRadius.keys.toList())
                                    .snapshots().collect {
                                        synchronized(lock) {
                                            val newTaskIds = it.documents.map { it.id }
                                            for (key in tasks.keys) {
                                                if (!newTaskIds.contains(key)) {
                                                    tasks.remove(key)
                                                }
                                            }
                                            for (doc in it.documents) {
                                                val location =
                                                    doc.getGeoPoint(TaskDbSchema.LOCATION)
                                                if (location == null) {
                                                    tasks.remove(doc.id)
                                                    continue
                                                }
                                                if (!tasks.contains(doc.id)) {
                                                    tasks[doc.id] = TaskLocationDto(
                                                        doc.id,
                                                        doc[TaskDbSchema.TITLE].toString(),
                                                        location.latitude,
                                                        location.longitude,
                                                        taskRadius[doc.id]!!,
                                                        false
                                                    )
                                                } else {
                                                    val oldTask = tasks[doc.id]!!
                                                    oldTask.title =
                                                        doc[TaskDbSchema.TITLE].toString()
                                                    oldTask.latitude = location.latitude
                                                    oldTask.longitude = location.longitude
                                                    oldTask.radius = taskRadius[doc.id]!!
                                                }
                                                notifyIfNeeded(tasks[doc.id]!!)
                                            }
                                        }
                                    }
                            }
                            listener?.start()
                        }
                    }
                }
        }
    }

    fun onLocationUpdated(latitude: Double, longitude: Double) {
        lastLatitude = latitude
        lastLongitude = longitude
        tasks.forEach { notifyIfNeeded(it.value) }
    }

    private fun notifyIfNeeded(task: TaskLocationDto) {
        lastLatitude ?: return
        lastLongitude ?: return
        val shouldNotify = countDistance(task.latitude, task.longitude) <= task.radius
        if (shouldNotify && !task.wasNotified) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val link = NavDeepLinkBuilder(context).setComponentName(MainActivity::class.java)
                    .setGraph(R.navigation.navigation).setDestination(R.id.showTaskInfoFragment)
                    .setArguments(bundleOf("taskId" to task.id)).createPendingIntent()
                val notification = NotificationCompat.Builder(context, "LOCATION")
                    .setContentIntent(link)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Не забудьте выполнить задачу!")
                    .setContentText("Неподалёку доступна задача ${task.title}")
                    .build()
                val manager = NotificationManagerCompat.from(context)
                manager.notify(System.currentTimeMillis().toInt(), notification)
                task.wasNotified = true
            }
        } else if (!shouldNotify && task.wasNotified) {
            task.wasNotified = false
        }
    }

    private fun countDistance(latitude: Double, longitude: Double): Double {
        val phi1 = latitude * Math.PI / 180
        val phi2 = lastLatitude!! * Math.PI / 180
        val latDiff = phi2 - phi1
        val longDiff = (longitude - lastLongitude!!) * Math.PI / 180
        val sinLat = sin(latDiff / 2)
        val sinLong = sin(longDiff / 2)
        val a = sinLat * sinLat + cos(phi1) * cos(phi2) * sinLong * sinLong
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return c * RADIUS
    }
}