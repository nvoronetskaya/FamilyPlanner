package com.familyplanner.events.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.events.data.Event
import com.familyplanner.events.data.EventRepository
import com.familyplanner.events.data.Invitation
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.tasks.model.TaskCreationStatus
import com.familyplanner.tasks.model.UserFile
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class NewEventViewModel : ViewModel() {
    private val familyRepo = FamilyRepository()
    private val userRepo = UserRepository()
    private val eventRepo = EventRepository()
    private val attendees = MutableSharedFlow<List<Invitation>>(replay = 1)
    private val userId = FamilyPlanner.userId
    private val addEvent = MutableSharedFlow<TaskCreationStatus>()
    private var familyId: String = ""

    init {
        viewModelScope.launch(Dispatchers.IO) {
            familyId = userRepo.getUserByIdOnce(userId).familyId ?: ""
            attendees.emit(
                familyRepo.getFamilyMembersOnce(familyId)
                    .map { Invitation(it.id, it.name, it.birthday, false) })
        }
    }

    fun getAttendees(): Flow<List<Invitation>> = attendees

    fun createEvent(
        name: String,
        description: String,
        start: Long,
        finish: Long,
        invitations: List<Invitation>,
        files: List<UserFile>,
        isConnected: Boolean
    ) {
        val event = Event("", name, description, start, finish, userId, familyId)
        viewModelScope.launch(Dispatchers.IO) {
            val eventId = eventRepo.addEvent(event)
            eventRepo.addInvitations(invitations, eventId)
            val result = if (files.isNotEmpty()) {
                if (!isConnected || !eventRepo.tryUploadFiles(files, eventId)) {
                    TaskCreationStatus.FILE_UPLOAD_FAILED
                } else {
                    TaskCreationStatus.SUCCESS
                }
            } else {
                TaskCreationStatus.SUCCESS
            }
            addEvent.emit(result)
        }
    }

    fun getCreationStatus(): Flow<TaskCreationStatus> = addEvent
}