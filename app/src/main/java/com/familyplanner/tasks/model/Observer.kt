package com.familyplanner.tasks.model

data class Observer(val userId: String, @JvmField val isExecutor: Boolean, val taskId: String) {
    constructor() : this("", false, "")
}
