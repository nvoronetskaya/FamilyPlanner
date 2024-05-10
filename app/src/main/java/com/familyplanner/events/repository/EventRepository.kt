package com.familyplanner.events.repository

import android.net.Uri
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.schema.EventAttendeeDbSchema
import com.familyplanner.common.schema.EventDbSchema
import com.familyplanner.common.schema.UserDbSchema
import com.familyplanner.events.data.Event
import com.familyplanner.events.data.EventAttendee
import com.familyplanner.events.data.EventAttendeeStatus
import com.familyplanner.events.data.Invitation
import com.familyplanner.tasks.data.UserFile
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
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
import java.util.UUID

class EventRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val scope = CoroutineScope(Dispatchers.IO)

    fun addEvent(event: Event): String {
        val eventId = UUID.randomUUID().toString()
        firestore.collection(EventDbSchema.EVENT_TABLE).document(eventId).set(event)
        return eventId
    }

    fun addInvitations(invitations: List<Invitation>, eventId: String) {
        for (invitation in invitations) {
            val attendeeData = mapOf<String, Any>(
                EventAttendeeDbSchema.EVENT_ID to eventId,
                EventAttendeeDbSchema.USER_ID to invitation.userId,
                EventAttendeeDbSchema.STATUS to if (invitation.userId != FamilyPlanner.userId) EventAttendeeStatus.UNKNOWN else EventAttendeeStatus.COMING
            )
            firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE).add(attendeeData)
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
        return firestore.collection(EventDbSchema.EVENT_TABLE).document(eventId).snapshots().map {
            it.toObject(Event::class.java)
        }
    }

    suspend fun getEventByIdOnce(eventId: String): Event? {
        val event = firestore.collection(EventDbSchema.EVENT_TABLE).document(eventId).get().await()
            .toObject(Event::class.java)
        event?.id = eventId
        return event
    }

    fun getEventAttendees(eventId: String): Flow<List<EventAttendee>> {
        return firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
            .whereEqualTo(EventAttendeeDbSchema.EVENT_ID, eventId).snapshots()
            .map { attendees ->
                attendees.map {
                    val userName =
                        firestore.collection(UserDbSchema.USER_TABLE)
                            .document(it[EventAttendeeDbSchema.USER_ID].toString()).get()
                            .await()[UserDbSchema.NAME].toString()
                    EventAttendee(
                        it[EventAttendeeDbSchema.EVENT_ID].toString(),
                        it[EventAttendeeDbSchema.USER_ID].toString(),
                        EventAttendeeStatus.valueOf(it[EventAttendeeDbSchema.STATUS].toString()),
                        userName
                    )
                }
            }
    }

    suspend fun getEventAttendeesOnce(eventId: String): List<EventAttendee> {
        return firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
            .whereEqualTo(EventAttendeeDbSchema.EVENT_ID, eventId).get().await()
            .map {
                val userName =
                    firestore.collection(UserDbSchema.USER_TABLE)
                        .document(it[EventAttendeeDbSchema.USER_ID].toString()).get()
                        .await()[UserDbSchema.NAME].toString()
                EventAttendee(
                    it[EventAttendeeDbSchema.EVENT_ID].toString(),
                    it[EventAttendeeDbSchema.USER_ID].toString(),
                    EventAttendeeStatus.valueOf(it[EventAttendeeDbSchema.STATUS].toString()),
                    userName
                )
            }
    }

    suspend fun changeComing(userId: String, eventId: String, isComing: Boolean) {
        firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
            .whereEqualTo(EventAttendeeDbSchema.USER_ID, userId)
            .whereEqualTo(EventAttendeeDbSchema.EVENT_ID, eventId).get()
            .await().documents.forEach {
                val status =
                    if (isComing) EventAttendeeStatus.COMING else EventAttendeeStatus.NOT_COMING
                it.reference.update(EventAttendeeDbSchema.STATUS, status)
            }
    }

    fun deleteEvent(eventId: String) {
        firestore.collection(EventDbSchema.EVENT_TABLE).document(eventId).delete()
            .continueWith {
                firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
                    .whereEqualTo(EventAttendeeDbSchema.EVENT_ID, eventId).get()
                    .continueWith {
                        it.result.documents.forEach { doc -> doc.reference.delete() }
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
            EventDbSchema.NAME to name,
            EventDbSchema.DESCRIPTION to description,
            EventDbSchema.START to start,
            EventDbSchema.FINISH to finish
        )
        firestore.collection(EventDbSchema.EVENT_TABLE).document(eventId).update(data).await()
        for (invitation in newInvitations) {
            if (invitation.isInvited != attendees[invitation.userId]?.isInvited) {
                if (invitation.isInvited) {
                    val newInvitation = mapOf<String, Any>(
                        EventAttendeeDbSchema.EVENT_ID to eventId,
                        EventAttendeeDbSchema.USER_ID to invitation.userId,
                        EventAttendeeDbSchema.STATUS to EventAttendeeStatus.UNKNOWN
                    )
                    firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
                        .add(newInvitation)
                } else {
                    firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
                        .whereEqualTo(EventAttendeeDbSchema.EVENT_ID, eventId)
                        .whereEqualTo(EventAttendeeDbSchema.USER_ID, invitation.userId).get()
                        .await().documents.forEach { it.reference.delete() }
                }
            }
        }
    }

    fun getEventsForPeriod(
        userId: String,
        start: Long,
        finish: Long
    ): Flow<List<Event>> {
        val userEvents = MutableSharedFlow<List<Event>>()
        scope.launch {
            firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
                .whereEqualTo(EventAttendeeDbSchema.USER_ID, userId).snapshots()
                .collect {
                    launch {
                        val eventsId =
                            it.documents.map { document -> document[EventAttendeeDbSchema.EVENT_ID].toString() }
                        if (eventsId.isEmpty()) {
                            userEvents.emit(listOf())
                        } else {
                            firestore.collection(EventDbSchema.EVENT_TABLE)
                                .whereIn(FieldPath.documentId(), eventsId)
                                .snapshots()
                                .collect {
                                    val documents =
                                        it.documents.filter { doc -> doc.getLong(EventDbSchema.START)!! in start..finish }
                                    val result = if (documents.isEmpty()) {
                                        listOf()
                                    } else {
                                        documents.map { doc ->
                                            val event = doc.toObject(Event::class.java)!!
                                            event.id = doc.id
                                            event
                                        }
                                    }
                                    userEvents.emit(result)
                                }
                        }
                    }
                }
        }
        return userEvents
    }

    fun getAttendingEventsForPeriod(
        userId: String,
        start: Long,
        finish: Long
    ): Flow<List<Event>> {
        val userEvents = MutableSharedFlow<List<Event>>()
        scope.launch {
            firestore.collection(EventAttendeeDbSchema.EVENT_ATTENDEE_TABLE)
                .whereEqualTo(EventAttendeeDbSchema.USER_ID, userId)
                .whereEqualTo(EventAttendeeDbSchema.STATUS, EventAttendeeStatus.COMING.name)
                .snapshots().collect {
                    launch {
                        val eventsId =
                            it.documents.map { document -> document[EventAttendeeDbSchema.EVENT_ID].toString() }
                        if (eventsId.isEmpty()) {
                            userEvents.emit(listOf())
                        } else {
                            firestore.collection(EventDbSchema.EVENT_TABLE)
                                .whereIn(FieldPath.documentId(), eventsId)
                                .snapshots()
                                .collect {
                                    val documents =
                                        it.documents.filter { doc -> doc.getLong(EventDbSchema.START)!! in start..finish }
                                    val result = if (documents.isEmpty()) {
                                        listOf()
                                    } else {
                                        documents.map { doc ->
                                            val event = doc.toObject(Event::class.java)!!
                                            event.id = doc.id
                                            event
                                        }
                                    }
                                    userEvents.emit(result)
                                }
                        }
                    }
                }
        }
        return userEvents
    }
}