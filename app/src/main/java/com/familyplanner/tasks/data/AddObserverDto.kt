package com.familyplanner.tasks.data

data class AddObserverDto(val userId: String, val userName: String, val birthday: String, var isObserver: Boolean, var isExecutor: Boolean)
