package com.familyplanner.events.data

import com.familyplanner.tasks.model.UserFile
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    fun addEvent(event: Event): Task<DocumentReference> {
        return firestore.collection("events").add(event)
    }

    fun addInvitations(invitations: List<Invitation>, eventId: String) {
        for (invitation in invitations) {
            val attendeeData = mapOf<String, Any>(
                "eventId" to eventId,
                "userId" to invitation.userId,
                "status" to EventAttendeeStatus.UNKNOWN
            )
            firestore.collection("eventAttendees").add(attendeeData)
        }
    }

    suspend fun tryUploadFiles(files: List<UserFile>, eventId: String): Boolean {
        var isSuccessful = true
        val filesRef = storage.reference.child(eventId)
        for (file in files) {
            val metadata = storageMetadata { setCustomMetadata("name", file.name) }
            if (filesRef.child(file.name).putFile(file.uri, metadata).await().error != null) {
                isSuccessful = false
            }
        }
        return isSuccessful
    }
}