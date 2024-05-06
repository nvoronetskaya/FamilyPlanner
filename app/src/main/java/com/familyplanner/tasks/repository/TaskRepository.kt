package com.familyplanner.tasks.repository

import android.net.Uri
import com.familyplanner.common.User
import com.familyplanner.tasks.dto.CommentDto
import com.familyplanner.tasks.dto.ObserverDto
import com.familyplanner.tasks.model.Importance
import com.google.android.gms.tasks.Task as GoogleTask
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.RepeatType
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.UserFile
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class TaskRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val scope = CoroutineScope(Dispatchers.IO)
    private val userTasks = MutableSharedFlow<List<Task>>()

    suspend fun getCommonTasksForUser(userId: String): Flow<List<Task>> {
        scope.launch {
            firestore.collection("observers")
                .whereEqualTo("userId", userId).snapshots().collect {
                    val tasksIds = it.map { it["taskId"].toString() }
                    launch {
                        if (tasksIds.isEmpty()) {
                            userTasks.emit(listOf())
                        } else {
                            firestore.collection("tasks").whereEqualTo("parentId", null).snapshots()
                                .collect { result ->
                                    val queryTasks = mutableListOf<Task>()
                                    for (doc in result.documents) {
                                        if (!tasksIds.contains(doc.id)) {
                                            continue
                                        }
                                        val task = doc.toObject(Task::class.java)!!
                                        task.id = doc.id
                                        queryTasks.add(task)
                                    }
                                    userTasks.emit(queryTasks)
                                }
                        }
                    }
                }
        }

        return userTasks
    }

    suspend fun getSharedTasks(userId: String, executorId: String): Flow<List<Task>> {
        if (userId.equals(executorId)) {
            return getCommonTasksForUser(userId)
        }
        scope.launch {
            firestore.collection("observers").whereEqualTo("userId", userId).snapshots().collect {
                val ids = it.map { it["taskId"].toString() }
                launch {
                    if (ids.isEmpty()) {
                        userTasks.emit(listOf())
                    } else {
                        firestore.collection("observers").whereEqualTo("userId", executorId)
                            .snapshots()
                            .collect { result ->
                                val tasksIds = result.filter {
                                    it.getBoolean("isExecutor") ?: false && ids.contains(
                                        it.getString(
                                            "taskId"
                                        )
                                    )
                                }.map { it["taskId"].toString() }
                                launch {
                                    if (tasksIds.isEmpty()) {
                                        userTasks.emit(listOf())
                                    } else {
                                        firestore.collection("tasks").whereEqualTo("parentId", null)
                                            .snapshots()
                                            .collect { result ->
                                                val queryTasks = mutableListOf<Task>()
                                                for (doc in result.documents) {
                                                    if (!tasksIds.contains(doc.id)) {
                                                        continue
                                                    }
                                                    val task = doc.toObject(Task::class.java)!!
                                                    task.id = doc.id
                                                    queryTasks.add(task)
                                                }
                                                userTasks.emit(queryTasks)
                                            }
                                    }
                                }
                            }
                    }
                }
            }
        }
        return userTasks
    }

    suspend fun getTaskByIdOnce(taskId: String): Task? {
        return firestore.collection("tasks").document(taskId).get().await()
            .toObject(Task::class.java)
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return firestore.collection("tasks").document(taskId).snapshots().map {
            val task = it.toObject(Task::class.java)
            task?.id = it.id
            task
        }
    }

    fun getTaskComments(taskId: String): Flow<List<CommentDto>> {
        return firestore.collection("comments").whereEqualTo("taskId", taskId).snapshots().map {
            val comments = mutableListOf<CommentDto>()
            for (doc in it.documents) {
                val userName =
                    firestore.collection("users").document(doc["userId"].toString()).get()
                        .await()["name"].toString()
                val files = storage.reference.child(doc.id).listAll().await().items.map { it.path }
                val comment = CommentDto(
                    doc.id,
                    doc["userId"].toString(),
                    userName,
                    doc.getLong("createdAt")!!,
                    doc["text"].toString(),
                    files
                )
                comments.add(comment)
            }
            comments
        }
    }

    fun getTaskObservers(taskId: String): Flow<List<ObserverDto>> {
        return firestore.collection("observers").whereEqualTo("taskId", taskId).snapshots().map {
            val users = mutableListOf<ObserverDto>()
            for (doc in it.documents) {
                val userName =
                    firestore.collection("users").document(doc["userId"].toString()).get()
                        .await()["name"].toString()
                val user = ObserverDto(
                    doc["userId"].toString(),
                    userName,
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

    fun addCreatorObserver(taskId: String, userId: String) {
        val observer = Observer(userId, false, taskId)
        firestore.collection("observers").add(observer)
    }

    suspend fun tryUploadFiles(
        files: List<UserFile>,
        taskId: String,
        prefix: String = "task"
    ): Boolean {
        var isSuccessful = true
        val filesRef = storage.reference.child("$prefix-$taskId")
        for (file in files) {
            val metadata = storageMetadata { setCustomMetadata("name", file.name) }
            if (filesRef.child(file.name).putFile(file.uri, metadata).await().error != null) {
                isSuccessful = false
            }
        }
        return isSuccessful
    }

    suspend fun updateTaskObservers(
        taskId: String,
        users: List<User>,
        observers: BooleanArray,
        executors: BooleanArray
    ) {
        val curObservers =
            firestore.collection("observers").whereEqualTo("taskId", taskId).get().await()
        curObservers.forEach { it.reference.delete() }
        for (i in users.indices) {
            if (!observers[i]) {
                continue
            }
            val observer = Observer(users[i].id, executors[i], taskId)
            firestore.collection("observers").add(observer)
        }
    }

    fun getSubtasks(taskId: String): Flow<List<Task>> {
        return firestore.collection("tasks").whereEqualTo("parentId", taskId).snapshots().map {
            val subTasks = mutableListOf<Task>()
            for (doc in it.documents) {
                val task = doc.toObject(Task::class.java) ?: continue
                task.id = doc.id
                subTasks.add(task)
            }
            subTasks
        }
    }

    fun getFilesForTask(taskId: String): GoogleTask<ListResult> {
        return storage.reference.child("task-$taskId").listAll()
    }

    fun downloadFile(path: String): GoogleTask<Uri> {
        return storage.reference.child(path).downloadUrl
    }

    fun addComment(userId: String, comment: String, taskId: String): GoogleTask<DocumentReference> {
        val data =
            mapOf<String, Any>(
                "userId" to userId,
                "text" to comment,
                "createdAt" to System.currentTimeMillis(),
                "taskId" to taskId
            )
        return firestore.collection("comments").add(data)
    }

    fun changeTaskCompleted(task: Task, isCompleted: Boolean, completedById: String) {
        val today = LocalDate.now().toEpochDay()
        if (isCompleted) {
            val data = mapOf<String, Any?>(
                "lastCompletionDate" to today,
                "previousCompletionDate" to task.lastCompletionDate
            )
            firestore.collection("tasks").document(task.id).update(data).addOnSuccessListener {
                val completion = mapOf<String, Any>(
                    "userId" to completedById,
                    "taskId" to task.id,
                    "completionDate" to today
                )
                firestore.collection("taskCompletion").add(completion)
            }
        } else {
            val data = mapOf<String, Any?>(
                "previousCompletionDate" to null,
                "lastCompletionDate" to task.previousCompletionDate
            )
            TODO()
            firestore.collection("tasks").document(task.id).update(data).addOnSuccessListener {
                firestore.collection("taskCompletion").whereEqualTo("taskId", task.id)
                    .whereEqualTo("completionDate", today).get().addOnCompleteListener {
                        it.result.documents.forEach { it.reference.delete() }
                    }
            }
        }
    }

    fun deleteTask(taskId: String) {
        val result = firestore.collection("tasks").document(taskId).delete().addOnSuccessListener {
            scope.launch {
                firestore.collection("observers").whereEqualTo("taskId", taskId).get()
                    .await().documents.forEach { it.reference.delete() }
                storage.reference.child("task-$taskId").delete()
            }
        }
    }

    fun getObserver(taskId: String, userId: String): Flow<Observer?> {
        return firestore.collection("observers").whereEqualTo("userId", userId)
            .whereEqualTo("taskId", taskId).limit(1).snapshots().map {
                it.documents.firstOrNull()?.toObject(Observer::class.java)
            }
    }

    suspend fun changeExecutorStatus(userId: String, taskId: String, isExecutor: Boolean) {
        firestore.collection("observers").whereEqualTo("userId", userId)
            .whereEqualTo("taskId", taskId).get().await()
            .forEach { it.reference.update("isExecutor", isExecutor) }
    }

    suspend fun addFile(taskId: String, file: UserFile): Boolean {
        val filesRef = storage.reference.child("task-$taskId")
        val metadata = storageMetadata { setCustomMetadata("name", file.name) }
        if (filesRef.child(file.name).putFile(file.uri, metadata).await().error != null) {
            return false
        }
        return true
    }

    fun removeFile(taskId: String, fileName: String): com.google.android.gms.tasks.Task<Void> {
        return storage.reference.child("task-$taskId").child(fileName).delete()
    }

    fun updateTask(taskId: String, taskData: Task) {
        val newValues = mutableMapOf<String, Any?>()
        newValues["title"] = taskData.title
        newValues["hasDeadline"] = taskData.hasDeadline
        newValues["deadline"] = taskData.deadline
        newValues["isContinuous"] = taskData.isContinuous
        newValues["startTime"] = taskData.startTime
        newValues["finishTime"] = taskData.finishTime
        newValues["repeatType"] = taskData.repeatType
        newValues["nDays"] = taskData.nDays
        newValues["daysOfWeek"] = taskData.daysOfWeek
        newValues["repeatStart"] = taskData.repeatStart
        newValues["importance"] = taskData.importance
        newValues["hasLocation"] = taskData.hasLocation
        newValues["location"] = taskData.location
        firestore.collection("tasks").document(taskId).update(newValues)
    }
}