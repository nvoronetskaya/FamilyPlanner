package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.common.User
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.tasks.model.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class TasksListViewModel : ViewModel() {
    private val tasks = MutableStateFlow<List<Task>>(listOf<Task>())
    private val user = MutableSharedFlow<User>(replay = 1)
    private val userRepo = UserRepository()
    private val familyRepo = FamilyRepository()
    private val taskRepo = TaskRepository()
    private val firestore = Firebase.firestore

    init {

    }

    fun getTasks(): Flow<List<Task>> = tasks
}