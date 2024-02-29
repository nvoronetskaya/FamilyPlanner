package com.familyplanner.tasks.repository

import com.familyplanner.common.User
import com.familyplanner.tasks.dto.CommentDto
import com.google.android.gms.tasks.Task as GoogleTask
import com.familyplanner.tasks.model.Comment
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.UserFile
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

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

    fun getTaskComments(taskId: String): Flow<List<Comment>> {
        firestore.collection("comments").whereEqualTo("taskId", taskId).snapshots().map {
            val comments = mutableListOf<Comment>()
            for (doc in it.documents) {
                val comment = Comment(
                    doc.id,
                    doc["text"].toString(),
                    doc["createdAt"].toString(),
                    doc["userId"].toString()
                )
                comments.add(comment)
            }
            comments
        }
    }

    fun getTaskObservers(taskId: String): Flow<List<Observer>> {
        firestore.collection("observers").whereEqualTo("familyId", familyId).snapshots().map {
            val users = mutableListOf<User>()
            for (doc in it.documents) {
                val user = User(
                    doc.id,
                    doc["name"].toString(),
                    doc["birthday"].toString(),
                    doc["hasFamily"] as Boolean,
                    doc["familyId"].toString(),
                    doc["email"].toString()
                )
                users.add(user)
            }
            users
        }
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

    fun addTaskObservers(
        taskId: String,
        users: List<User>,
        observers: BooleanArray,
        executors: BooleanArray
    ) {
        for (i in users.indices) {
            if (!observers[i]) {
                continue
            }
            val observer = Observer(users[i].id, executors[i], taskId)
            firestore.collection("observers").add(observer)
        }
    }
}