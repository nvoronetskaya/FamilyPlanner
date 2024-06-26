package com.familyplanner.common.background

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.common.schema.UserDbSchema
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
        val navigateTo: Int?
        if (sourceType.equals("TASK")) {
            key = "taskId"
            navigateTo = R.id.showTaskInfoFragment
        } else if (sourceType.equals("EVENT")) {
            key = "eventId"
            navigateTo = if (sourceId != null) R.id.eventInfoFragment else null
        } else if (sourceType.equals("LIST")) {
            key = "listId"
            navigateTo = R.id.groceryListInfoFragment
        } else {
            return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val notification =
                NotificationCompat.Builder(this, "DATA_UPDATES").setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentText(body)
            navigateTo?.let {
                val link = NavDeepLinkBuilder(this).setComponentName(MainActivity::class.java)
                    .setGraph(R.navigation.navigation).setArguments(bundleOf(key to sourceId))
                    .setDestination(navigateTo)
                notification.setContentIntent(link.createPendingIntent())
            }
            val manager = NotificationManagerCompat.from(this)
            manager.notify(System.currentTimeMillis().toInt(), notification.build())
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FamilyPlanner.userId.isEmpty()) {
            return
        }
        firestore.collection(UserDbSchema.USER_TABLE).document(FamilyPlanner.userId)
            .update(UserDbSchema.FCM_TOKEN, token)
    }
}