package com.familyplanner.events.data

import android.net.Uri
import com.familyplanner.tasks.model.UserFile
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        val filesRef = storage.reference.child("event-$eventId")
        for (file in files) {
            val metadata = storageMetadata { setCustomMetadata("name", file.name) }
            if (filesRef.child(file.name).putFile(file.uri, metadata).await().error != null) {
                isSuccessful = false
            }
        }
        return isSuccessful
    }

    fun getEventById(eventId: String): Flow<Event?> {
        return firestore.collection("events").document(eventId).snapshots().map {
            it.toObject(Event::class.java)
        }
    }

    suspend fun getEventByIdOnce(eventId: String): Event? {
        return firestore.collection("events").document(eventId).get().await()
            .toObject(Event::class.java)
    }

    fun getEventAttendees(eventId: String): Flow<List<EventAttendee>> {
        return firestore.collection("eventAttendees").whereEqualTo("eventId", eventId).snapshots()
            .map {
                it.toObjects(EventAttendee::class.java)
            }
    }

    suspend fun getEventAttendeesOnce(eventId: String): List<EventAttendee> {
        return firestore.collection("eventAttendees").whereEqualTo("eventId", eventId).get().await()
            .map {
                it.toObject(EventAttendee::class.java)
            }
    }

    suspend fun changeComing(userId: String, eventId: String, isComing: Boolean) {
        firestore.collection("eventAttendees").whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId).get()
            .await().documents.forEach { it.reference.delete() }
    }

    fun deleteEvent(eventId: String) {
        firestore.collection("events").document(eventId).delete().addOnSuccessListener {
            firestore.collection("eventAttendees").whereEqualTo("eventId", eventId).get()
                .addOnCompleteListener {
                    it.result.documents.forEach { it.reference.delete() }
                }
            storage.reference.child("event-$eventId").delete()
        }
    }

    fun downloadFile(path: String): Task<Uri> {
        return storage.reference.child(path).downloadUrl
    }

    fun getFilesForEvent(eventId: String): Task<ListResult> {
        return storage.reference.child("event-$eventId").listAll()
    }

    suspend fun addFile(eventId: String, file: UserFile): Boolean {
        val filesRef = storage.reference.child("event-$eventId")
        val metadata = storageMetadata { setCustomMetadata("name", file.name) }
        if (filesRef.child(file.name).putFile(file.uri, metadata).await().error != null) {
            return false
        }
        return true
    }

    fun removeFile(eventId: String, fileName: String): Task<Void> {
        return storage.reference.child("event-$eventId").child(fileName).delete()
    }

    fun updateEvent(
        eventId: String,
        name: String,
        description: String,
        start: Long,
        finish: Long,
        newInvitations: List<Invitation>,
        attendees: Map<String, Invitation>
    ) {
        val data = mapOf<String, Any>(
            "name" to name,
            "description" to description,
            "start" to start,
            "finish" to finish
        )
        firestore.collection("events").document(eventId).update(data).addOnSuccessListener {
            for (invitation in newInvitations) {
                if (invitation.isInvited != attendees[invitation.userId]?.isInvited) {
                    firestore.collection("eventAttendees").whereEqualTo("userId", invitation.userId)
                        .whereEqualTo("eventId", eventId).get().addOnSuccessListener {
                            it.documents.forEach { it.reference.delete() }
                            if (invitation.isInvited) {
                                val newInvitation = mapOf<String, Any>(
                                    "eventId" to eventId,
                                    "userId" to invitation.userId,
                                    "status" to EventAttendeeStatus.UNKNOWN
                                )
                                firestore.collection("eventAttendees").add(newInvitation)
                            }
                        }
                }
            }
        }
    }
}