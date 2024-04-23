package com.familyplanner.tasks.dto
data class ObserverDto(val userId: String, val userName: String, @JvmField val isExecutor: Boolean, val taskId: String)