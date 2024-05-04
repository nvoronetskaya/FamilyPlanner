package com.familyplanner.location.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.familyplanner.FamilyPlanner
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationNotificationSender(val context: Context) {
    private val tasks: HashMap<String, TaskLocationDto> = hashMapOf()
    private val firestore = Firebase.firestore
    private var listener: ListenerRegistration? = null
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private val RADIUS = 6371e3

    init {
        firestore.collection("observers").whereEqualTo("userId", FamilyPlanner.userId)
            .whereNotEqualTo("radius", null).addSnapshotListener { value, error ->
                if (error != null || value == null) {
                    return@addSnapshotListener
                }
                val taskRadius = hashMapOf<String, Double>()
                value.documents.forEach {
                    taskRadius[it["taskId"].toString()] = it.getDouble("radius")!!
                }
                if (taskRadius.isNotEmpty()) {
                    listener?.remove()
                    listener = firestore.collection("tasks")
                        .whereIn(FieldPath.documentId(), taskRadius.keys.toList())
                        .addSnapshotListener { value, error ->
                            if (value == null || error != null) {
                                return@addSnapshotListener
                            }
                            val newTaskIds = value.documents.map { it.id }
                            for (key in tasks.keys) {
                                if (!newTaskIds.contains(key)) {
                                    tasks.remove(key)
                                }
                            }
                            for (doc in value.documents) {
                                val location = doc.getGeoPoint("location")
                                if (location == null) {
                                    tasks.remove(doc.id)
                                    continue
                                }
                                if (!tasks.contains(doc.id)) {
                                    tasks[doc.id] = TaskLocationDto(
                                        doc.id,
                                        doc["name"].toString(),
                                        location.latitude,
                                        location.longitude,
                                        taskRadius[doc.id]!!,
                                        false
                                    )
                                } else {
                                    val oldTask = tasks[doc.id]!!
                                    oldTask.title = doc["name"].toString()
                                    oldTask.latitude = location.latitude
                                    oldTask.longitude = location.longitude
                                    oldTask.radius = taskRadius[doc.id]!!
                                }
                                notifyIfNeeded(tasks[doc.id]!!)
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
                    .setSmallIcon(R.drawable.notifications)
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