package com.familyplanner.common.background

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.familyplanner.FamilyPlanner
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {
    private val firestore = Firebase.firestore

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.isEmpty()) {
            return
        }
        val sourceType = message.data["type"]
        val sourceId = message.data["sourceId"]
        val title = message.data["title"]
        val body = message.data["body"]
        val key: String
        val navigateTo: Int
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
                NotificationCompat.Builder(this, "DATA_UPDATES").setContentTitle(title)
                    .setSmallIcon(R.drawable.notifications)
                    .setContentText(body)
                    .setContentIntent(link).build()
            val manager = NotificationManagerCompat.from(this)
            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FamilyPlanner.userId.isEmpty()) {
            return
        }
        firestore.collection("users").document(FamilyPlanner.userId).update("fcmToken", token)
    }
}