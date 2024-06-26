package com.familyplanner.family.data

import com.familyplanner.family.data.ApplicationStatus

data class Application(var userId: String, var familyId: String, val status: ApplicationStatus = ApplicationStatus.NEW) {
    constructor() : this(
        "", "", ApplicationStatus.NEW
    )
}