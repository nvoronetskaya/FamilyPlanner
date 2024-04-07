package com.familyplanner.tasks.dto

import com.familyplanner.tasks.model.Task

data class TaskWithDate(val task: Task, var date: Long?)
