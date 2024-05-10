package com.familyplanner.tasks.data
data class ObserverDto(val userId: String, val userName: String, @JvmField val isExecutor: Boolean, val taskId: String)