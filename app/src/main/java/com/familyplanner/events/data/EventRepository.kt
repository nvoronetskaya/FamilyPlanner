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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val scope = CoroutineScope(Dispatchers.IO)
    private val userEvents = MutableSharedFlow<List<Event>>()

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
        val event = firestore.collection("events").document(eventId).get().await()
            .toObject(Event::class.java)
        event?.id = eventId
        return event
    }

    fun getEventAttendees(eventId: String): Flow<List<EventAttendee>> {
        return firestore.collection("eventAttendees").whereEqualTo("eventId", eventId).snapshots()
            .map {
                it.map {
                    val userName =
                        firestore.collection("users").document(it["userId"].toString()).get()
                            .await()["name"].toString()
                    EventAttendee(
                        it["eventId"].toString(),
                        it["userId"].toString(),
                        EventAttendeeStatus.valueOf(it["status"].toString()),
                        userName
                    )
                }
            }
    }

    suspend fun getEventAttendeesOnce(eventId: String): List<EventAttendee> {
        return firestore.collection("eventAttendees").whereEqualTo("eventId", eventId).get().await()
            .map {
                val userName =
                    firestore.collection("users").document(it["userId"].toString()).get()
                        .await()["name"].toString()
                EventAttendee(
                    it["eventId"].toString(),
                    it["userId"].toString(),
                    EventAttendeeStatus.valueOf(it["status"].toString()),
                    userName
                )
            }
    }

    suspend fun changeComing(userId: String, eventId: String, isComing: Boolean) {
        firestore.collection("eventAttendees").whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId).get()
            .await().documents.forEach {
                val status =
                    if (isComing) EventAttendeeStatus.COMING else EventAttendeeStatus.NOT_COMING
                it.reference.update("status", status)
            }
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

    suspend fun updateEvent(
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
        firestore.collection("events").document(eventId).update(data).await()
        for (invitation in newInvitations) {
            if (invitation.isInvited != attendees[invitation.userId]?.isInvited) {
                if (invitation.isInvited) {
                    val newInvitation = mapOf<String, Any>(
                        "eventId" to eventId,
                        "userId" to invitation.userId,
                        "status" to EventAttendeeStatus.UNKNOWN
                    )
                    firestore.collection("eventAttendees").add(newInvitation)
                } else {
                    firestore.collection("eventAttendees")
                        .whereEqualTo("eventId", eventId)
                        .whereEqualTo("userId", invitation.userId).get()
                        .await().documents.forEach { it.reference.delete() }
                }
            }
        }
    }

    fun getEventsForPeriod(start: Long, finish: Long): Flow<List<Event>> {
        scope.launch {
            firestore.collection("events").whereGreaterThanOrEqualTo("finish", start).snapshots()
                .collect {
                    val documents = it.documents.filter { it.getLong("start")!! <= finish }
                    val result = if (documents.isEmpty()) {
                        listOf()
                    } else {
                        documents.map {
                            val event = it.toObject(Event::class.java)!!
                            event.id = it.id
                            event
                        }
                    }
                    userEvents.emit(result)
                }
        }
        return userEvents
    }
}