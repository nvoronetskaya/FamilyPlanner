package com.familyplanner.family.data

data class Family(var id: String, var name: String, var createdBy: String) {
    constructor() : this(
        "", "", ""
    )
}