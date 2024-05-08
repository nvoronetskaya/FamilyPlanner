package com.familyplanner.common

import com.google.firebase.firestore.GeoPoint

data class User(
    var id: String,
    var name: String,
    var birthday: String,
    var familyId: String?,
    var email: String,
    val location: GeoPoint? = null
) {
    constructor() : this(
        "", "",
        "", "", ""
    )
}