package com.familyplanner.events.data

data class EventAttendee(val eventId: String, val userId: String, val status: EventAttendeeStatus, val userName: String)
