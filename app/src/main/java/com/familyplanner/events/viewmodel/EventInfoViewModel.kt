package com.familyplanner.events.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.events.data.Event
import com.familyplanner.events.data.EventAttendee
import com.familyplanner.events.data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class EventInfoViewModel : ViewModel() {
    private var eventId: String = ""
    private val event = MutableSharedFlow<Event?>()
    private val attendees = MutableSharedFlow<List<EventAttendee>>()
    private val eventRepo = EventRepository()
    fun setEvent(eventId: String) {
        this.eventId = eventId
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                eventRepo.getEventById(eventId).collect {
                    event.emit(it)
                }
            }
            launch {
                eventRepo.getEventAttendees(eventId).collect {
                    attendees.emit(it)
                }
            }
        }
    }

    fun getEvent(): Flow<Event?> = event

    fun getAttendees(): Flow<List<EventAttendee>> = attendees
}