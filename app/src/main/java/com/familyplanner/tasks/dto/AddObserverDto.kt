package com.familyplanner.tasks.dto

data class AddObserverDto(val userId: String, val userName: String, val birthday: String, var isObserver: Boolean, var isExecutor: Boolean)
