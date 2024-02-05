package com.familyplanner.tasks.repository

import com.familyplanner.common.User
import com.familyplanner.tasks.model.Comment
import com.familyplanner.tasks.model.Observer
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

        TODO()
        return tasks
    }

    fun getTaskById(taskId: String): Flow<Task> {
        TODO()
    }

    fun getTaskComments(userId: String): Flow<List<Comment>> {
        TODO()
    }

    fun getTaskObservers(taskId: String): Flow<List<Observer>> {
        TODO()
    }
}