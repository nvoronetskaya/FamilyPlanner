package com.familyplanner.common

data class Application(var userId: String, var familyId: String) {
    constructor() : this(
        "", ""
    )
}