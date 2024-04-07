package com.familyplanner.tasks.model

import com.google.firebase.firestore.GeoPoint

data class Task(
    var id: String,
    var title: String,
    var hasDeadline: Boolean,
    var deadline: Long?,
    var isContinuous: Boolean,
    var startTime: Int,
    var finishTime: Int,
    var repeatType: RepeatType,
    var nDays: Int,
    var daysOfWeek: Int,
    var repeatStart: Long?,
    var importance: Importance,
    var hasLocation: Boolean,
    var location: GeoPoint?,
    var isPrivate: Boolean,
    var createdBy: String,
    var familyId: String,
    var lastCompletionDate: Long?,
    var parentId: String?
) {
    constructor() : this(
        "", "", false, null, false, 0,
        0, RepeatType.ONCE, 0, 0, null, Importance.HIGH,
        false, GeoPoint(0.0, 0.0), false, "", "", null, null
    )
}
