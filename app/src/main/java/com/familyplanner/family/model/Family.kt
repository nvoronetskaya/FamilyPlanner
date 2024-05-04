package com.familyplanner.family.model

data class Family(var id: String, var name: String, var code: String, var createdBy: String) {
    constructor() : this(
        "", "", "", ""
    )
}