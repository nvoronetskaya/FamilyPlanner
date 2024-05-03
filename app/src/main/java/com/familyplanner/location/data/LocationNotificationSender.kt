package com.familyplanner.location.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LocationNotificationSender(val context: Context) {
    private val tasks: HashMap<String, TaskLocationDto> = hashMapOf()
    private val firestore = Firebase.firestore
    private var listener: ListenerRegistration? = null
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

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
        val latitudeDiff = (lastLatitude!! - task.latitude)
        val longitudeDiff = (lastLongitude!! - task.longitude)
        val shouldNotify =
            latitudeDiff * latitudeDiff + longitudeDiff * longitudeDiff <= task.radius * task.radius
        if (shouldNotify && !task.wasNotified) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val notification = NotificationCompat.Builder(context, "LOCATION")
                    .setSmallIcon(R.drawable.notifications)
                    .setContentTitle("Не забудьте выполнить задачу!")
                    .setContentText("Неподалёку доступна задача ${task.title}").setOngoing(true)
                    .build()
                val manager = NotificationManagerCompat.from(context)
                manager.notify(System.currentTimeMillis().toInt(), notification)
                task.wasNotified = true
            }
        } else if (!shouldNotify && task.wasNotified) {
            task.wasNotified = false
        }
    }
}