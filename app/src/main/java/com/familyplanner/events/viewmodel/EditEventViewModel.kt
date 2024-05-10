package com.familyplanner.events.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.events.data.Event
import com.familyplanner.events.repository.EventRepository
import com.familyplanner.events.data.Invitation
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.tasks.data.UserFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditEventViewModel : ViewModel() {
    private var eventId: String = ""
    private var event: Event? = null
    private val eventRepo = EventRepository()
    private val familyRepo = FamilyRepository()
    private var files = mutableListOf<UserFile>()
    private var attendees = mutableMapOf<String, Invitation>()
    private var initialAttendees = mutableMapOf<String, Invitation>()
    private var isFileUploadSuccessful: Boolean = true

    suspend fun prepareData(eventId: String, familyId: String) {
        if (eventId == this.eventId) {
            return
        }

        this.eventId = eventId
        event = eventRepo.getEventByIdOnce(eventId)
        val allMembers = familyRepo.getFamilyMembersOnce(familyId)
        val invitations = eventRepo.getEventAttendeesOnce(eventId)
        attendees.clear()
        attendees.putAll(allMembers.map { user ->
            user.id to Invitation(
                user.id,
                user.name,
                user.birthday,
                invitations.firstOrNull { it.userId == user.id } != null)
        })
        initialAttendees.clear()
        initialAttendees.putAll(allMembers.map { user ->
            user.id to Invitation(
                user.id,
                user.name,
                user.birthday,
                invitations.firstOrNull { it.userId == user.id } != null)
        })
        files.clear()
        files.addAll(eventRepo.getFilesForEvent(eventId).await().items.map {
            UserFile(
                Uri.EMPTY,
                it.name,
                0.0
            )
        })
    }

    fun getEvent(): Event? = event

    fun removeFile(fileName: String) {
        eventRepo.removeFile(eventId, fileName)
    }

    fun addFile(file: UserFile) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!eventRepo.addFile(eventId, file)) {
                isFileUploadSuccessful = false
            }
        }
    }

    fun getAttendees(): List<Invitation> = attendees.values.toList()

    fun updateEventInfo(
        name: String,
        description: String,
        start: Long,
        finish: Long,
        newInvitations: List<Invitation>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            eventRepo.updateEvent(
                eventId,
                name,
                description,
                start,
                finish,
                newInvitations,
                initialAttendees
            )

        }
    }

    fun getIsFilesUploadSuccessful() = isFileUploadSuccessful

    fun getFiles(): List<UserFile> = files
}