package com.familyplanner.location.data

import com.google.firebase.firestore.GeoPoint

data class TaskLocationDto(val id: String, var title: String, var latitude: Double, var longitude: Double, var radius: Double, var wasNotified: Boolean)
