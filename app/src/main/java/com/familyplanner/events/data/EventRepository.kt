package com.familyplanner.events.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class EventRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    fun addEvent(event: Event) {
        firestore.collection("events").add(event) 
    }
}