package com.familyplanner.tasks.model

import com.google.firebase.firestore.GeoPoint

data class Task(
    var id: String,
    var title: String,
    var deadline: Long?,
    @JvmField var isContinuous: Boolean,
    var startTime: Int,
    var finishTime: Int,
    var repeatType: RepeatType,
    var nDays: Int,
    var daysOfWeek: Int,
    var repeatStart: Long?,
    var importance: Importance,
    var location: GeoPoint?,
    var address: String?,
    var createdBy: String,
    var familyId: String,
    var previousCompletionDate: Long?,
    var lastCompletionDate: Long?,
    var parentId: String?
) {
    constructor() : this(
        "",
        "",
        null,
        false,
        0,
        0,
        RepeatType.ONCE,
        0,
        0,
        null,
        Importance.HIGH,
        GeoPoint(0.0, 0.0),
        "",
        "",
        "",
        null,
        null,
        null
    )
}
