package com.familyplanner.tasks.repository

import com.familyplanner.common.User
import com.google.android.gms.tasks.Task as GoogleTask
import com.familyplanner.tasks.model.Comment
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.UserFile
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
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
        firestore.collection("observers").whereEqualTo("userId", userId).snapshots().map {
            val taskIds = it.map { task -> task.id }
            firestore.collection("tasks").whereIn(FieldPath.documentId(), taskIds).snapshots()
                .map { result ->
                    val queryTasks = mutableListOf<Task>()
                    for (doc in result.documents) {
                        val task = doc.toObject(Task::class.java)!!
                        task.id = doc.id
                    }
                    tasks.emit(queryTasks)
                }
        }
        return tasks
    }

    fun getSharedTasks(userId: String, executorId: String): Flow<List<Task>> {
        if (userId.equals(executorId)) {
            return getCommonTasksForUser(userId)
        }
        val tasks = MutableSharedFlow<List<Task>>()
        firestore.collection("observers").whereEqualTo("userId", userId).snapshots().map {
            val ids = it.map { task -> task.id }
            firestore.collection("observers").whereEqualTo("userId", executorId)
                .whereEqualTo("isExecutor", true).whereIn("taskId", ids).snapshots().map { result ->
                    val queryTasks = mutableListOf<Task>()
                    for (doc in result.documents) {
                        val task = doc.toObject(Task::class.java)!!
                        task.id = doc.id
                    }
                    tasks.emit(queryTasks)
                }
        }
        return tasks
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return firestore.collection("tasks").document(taskId).snapshots().map {
            val task = it.toObject(Task::class.java)
            task?.id = it.id
            task
        }
    }

    fun getTaskComments(taskId: String): Flow<List<Comment>> {
        return firestore.collection("comments").whereEqualTo("taskId", taskId).snapshots().map {
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
        return firestore.collection("observers").whereEqualTo("taskId", taskId).snapshots().map {
            val users = mutableListOf<Observer>()
            for (doc in it.documents) {
                val user = Observer(
                    doc["userId"].toString(),
                    doc.getBoolean("isExecutor") ?: false,
                    doc["taskId"].toString()
                )
                users.add(user)
            }
            users
        }
    }

    fun addTask(task: Task): GoogleTask<DocumentReference> {
        return firestore.collection("tasks").add(task)
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