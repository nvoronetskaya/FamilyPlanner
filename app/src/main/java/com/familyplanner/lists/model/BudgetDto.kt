package com.familyplanner.lists.model

import java.time.LocalDateTime

data class BudgetDto (val addedAt: LocalDateTime, val listName: String?, val userName: String, val sumSpent: Double)