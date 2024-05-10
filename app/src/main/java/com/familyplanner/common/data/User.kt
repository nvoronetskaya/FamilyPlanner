package com.familyplanner.common.data

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