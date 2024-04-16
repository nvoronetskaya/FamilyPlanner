package com.familyplanner.common.background

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NewTaskNotificationService : FirebaseMessagingService() {
    private val auth = Firebase.auth
    override fun onMessageReceived(message: RemoteMessage) {
        val userId = auth.currentUser?.uid ?: return
        if (message.data.isEmpty()) {
            return
        }
        val sourceType = message.data["type"]
        val sourceId = message.data["sourceId"]
        val title = message.data["title"]
        val body = message.data["body"]
        var key = ""
        var navigateTo = 0
        if (sourceType.equals("TASK")) {
            key = "taskId"
            navigateTo = R.id.showTaskInfoFragment
        } else if (sourceType.equals("EVENT")) {
            key = "eventId"
            navigateTo = R.id.eventInfoFragment
        } else {
            return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val link = NavDeepLinkBuilder(this).setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.navigation).setDestination(navigateTo)
                .setArguments(bundleOf(key to sourceId)).createPendingIntent()
            val notification =
                NotificationCompat.Builder(this, "10").setContentTitle(title).setSmallIcon(R.drawable.notifications)
                    .setContentText(body)
                    .setContentIntent(link).build()
            val manager = NotificationManagerCompat.from(this)
            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}