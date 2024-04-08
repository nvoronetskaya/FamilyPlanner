package com.familyplanner.events.data

data class EventDto(val event: Event, val attendees: List<EventAttendee>, val files: List<String>) 
