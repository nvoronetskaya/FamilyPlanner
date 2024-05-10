package com.familyplanner.tasks.data

data class Observer(val userId: String, @JvmField val isExecutor: Boolean, val taskId: String) {
    constructor() : this("", false, "")
}
