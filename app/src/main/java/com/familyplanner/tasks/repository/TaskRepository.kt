package com.familyplanner.tasks.repository

import com.google.android.gms.tasks.Task as GoogleTask
import com.familyplanner.tasks.model.Comment
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.UserFile
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class TaskRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

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

    fun addTask(task: Task): GoogleTask<DocumentReference> {
        return firestore.collection(("tasks")).add(task)
    }

    fun uploadFiles(files: List<UserFile>, taskId: String): List<UploadTask> {
        val tasks = mutableListOf<UploadTask>()
        val filesRef = storage.reference.child(taskId)
        for (file in files) {
            val metadata = storageMetadata { setCustomMetadata("name", file.name) }
            tasks.add(filesRef.putFile(file.uri, metadata))
        }
        return tasks
    }
}