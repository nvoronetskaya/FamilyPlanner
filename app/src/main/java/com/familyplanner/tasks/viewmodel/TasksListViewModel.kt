package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.familyplanner.tasks.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TasksListViewModel : ViewModel() {
    private val tasks = MutableStateFlow<List<Task>>(listOf<Task>())

    fun getTasks(): Flow<List<Task>> = tasks
}