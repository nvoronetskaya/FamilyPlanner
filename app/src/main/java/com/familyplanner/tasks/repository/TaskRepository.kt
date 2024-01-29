package com.familyplanner.tasks.repository

import com.familyplanner.tasks.model.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class TaskRepository {
    private val firestore = Firebase.firestore

    fun getCommonTasksForUser(userId: String): Flow<List<Task>> {
        val tasks = MutableSharedFlow<List<Task>>()

//        firestore.collection("users").where
        return tasks
    }
}