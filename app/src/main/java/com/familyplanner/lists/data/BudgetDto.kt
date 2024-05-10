package com.familyplanner.lists.data

import java.time.LocalDateTime

data class BudgetDto(
    val addedAt: LocalDateTime,
    val listName: String?,
    val listId: String,
    val userName: String,
    val sumSpent: Double
)