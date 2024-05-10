package com.familyplanner.tasks.data


data class TaskObserver(
    var id: String,
    var userId: String,
    var taskId: String,
    var isExecutor: Boolean,
    var sendLocationNotifications: Boolean,
    var radius: Int,
    var shouldNotify: Boolean,
    var notifyAt: Int,
    var notifyDaily: Boolean
) {
    constructor() : this(
        "", "", "", false, false,
        0, false, 0, false
    )
}