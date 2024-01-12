package com.familyplanner.common

data class User(
    var id: String,
    var name: String,
    var birthday: String,
    var hasFamily: Boolean,
    var familyId: String?,
    var email: String
)