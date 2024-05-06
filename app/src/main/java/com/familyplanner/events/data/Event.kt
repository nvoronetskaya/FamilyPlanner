package com.familyplanner.events.data

data class Event(
    var id: String,
    val name: String,
    val description: String,
    val start: Long,
    val finish: Long,
    val createdBy: String,
    val familyId: String
) {
    constructor() : this("", "", "", 0, 0, "", "")
}