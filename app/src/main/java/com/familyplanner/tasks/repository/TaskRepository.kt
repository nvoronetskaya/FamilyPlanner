package com.familyplanner.tasks.repository

import android.net.Uri
import com.familyplanner.common.schema.CommentDbSchema
import com.familyplanner.common.schema.CompletionDbSchema
import com.familyplanner.common.schema.ObserverDbSchema
import com.familyplanner.common.schema.TaskDbSchema
import com.familyplanner.common.schema.UserDbSchema
import com.familyplanner.tasks.data.AddObserverDto
import com.familyplanner.tasks.data.CommentDto
import com.familyplanner.tasks.data.ObserverDto
import com.google.android.gms.tasks.Task as GoogleTask
import com.familyplanner.tasks.data.Observer
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.Task
import com.familyplanner.tasks.data.UserFile
import com.google.firebase.Firebase
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.pow

class TaskRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val scope = CoroutineScope(Dispatchers.IO)
    private val userTasks = MutableSharedFlow<List<Task>>()

    private suspend fun getCommonTasksForUser(userId: String): Flow<List<Task>> {
        scope.launch {
            firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
                .whereEqualTo(ObserverDbSchema.USER_ID, userId).snapshots().collect {
                    val tasksIds = it.map { it[ObserverDbSchema.TASK_ID].toString() }
                    launch {
                        if (tasksIds.isEmpty()) {
                            userTasks.emit(listOf())
                        } else {
                            firestore.collection(TaskDbSchema.TASK_TABLE).snapshots()
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
            firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
                .whereEqualTo(ObserverDbSchema.USER_ID, userId).snapshots().collect {
                    val ids = it.map { it[ObserverDbSchema.TASK_ID].toString() }
                    launch {
                        if (ids.isEmpty()) {
                            userTasks.emit(listOf())
                        } else {
                            firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
                                .whereEqualTo(ObserverDbSchema.USER_ID, executorId)
                                .snapshots()
                                .collect { result ->
                                    val tasksIds = result.filter {
                                        it.getBoolean(ObserverDbSchema.EXECUTOR) ?: false && ids.contains(
                                            it.getString(
                                                ObserverDbSchema.TASK_ID
                                            )
                                        )
                                    }.map { it[ObserverDbSchema.TASK_ID].toString() }
                                    launch {
                                        if (tasksIds.isEmpty()) {
                                            userTasks.emit(listOf())
                                        } else {
                                            firestore.collection(TaskDbSchema.TASK_TABLE)
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
        return firestore.collection(TaskDbSchema.TASK_TABLE).document(taskId).get().await()
            .toObject(Task::class.java)
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return firestore.collection(TaskDbSchema.TASK_TABLE).document(taskId).snapshots().map {
            val task = it.toObject(Task::class.java)
            task?.id = it.id
            task
        }
    }

    fun getTaskComments(taskId: String): Flow<List<CommentDto>> {
        return firestore.collection(CommentDbSchema.COMMENT_TABLE)
            .whereEqualTo(CommentDbSchema.TASK_ID, taskId).snapshots().map {
                val comments = mutableListOf<CommentDto>()
                for (doc in it.documents) {
                    val userName =
                        firestore.collection(UserDbSchema.USER_TABLE)
                            .document(doc[CommentDbSchema.USER_ID].toString()).get()
                            .await()[UserDbSchema.NAME].toString()
                    val files =
                        storage.reference.child("comment-${doc.id}").listAll()
                            .await().items.map { it.name }
                    val comment = CommentDto(
                        doc.id,
                        doc[CommentDbSchema.USER_ID].toString(),
                        userName,
                        doc.getLong(CommentDbSchema.CREATED_AT)!!,
                        doc[CommentDbSchema.TEXT].toString(),
                        files
                    )
                    comments.add(comment)
                }
                comments
            }
    }

    fun getTaskObservers(taskId: String): Flow<List<ObserverDto>> {
        return firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
            .whereEqualTo(ObserverDbSchema.TASK_ID, taskId).snapshots().map {
                val users = mutableListOf<ObserverDto>()
                for (doc in it.documents) {
                    val userName =
                        firestore.collection(UserDbSchema.USER_TABLE)
                            .document(doc[ObserverDbSchema.USER_ID].toString()).get()
                            .await()[UserDbSchema.NAME].toString()
                    val user = ObserverDto(
                        doc[ObserverDbSchema.USER_ID].toString(),
                        userName,
                        doc.getBoolean(ObserverDbSchema.EXECUTOR) ?: false,
                        doc[ObserverDbSchema.TASK_ID].toString()
                    )
                    users.add(user)
                }
                users
            }
    }

    suspend fun getTaskObserversOnce(taskId: String): List<ObserverDto> {
        return firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
            .whereEqualTo(ObserverDbSchema.TASK_ID, taskId).get().await().map { doc ->
                val userName =
                    firestore.collection(UserDbSchema.USER_TABLE)
                        .document(doc[ObserverDbSchema.USER_ID].toString()).get()
                        .await()[UserDbSchema.NAME].toString()
                ObserverDto(
                    doc[ObserverDbSchema.USER_ID].toString(),
                    userName,
                    doc.getBoolean(ObserverDbSchema.EXECUTOR) ?: false,
                    doc[ObserverDbSchema.TASK_ID].toString()
                )
            }
    }

    fun addTask(task: Task) {
        firestore.collection(TaskDbSchema.TASK_TABLE).document(task.id).set(task)
    }

    fun addCreatorObserver(taskId: String, userId: String) {
        val observer = Observer(userId, false, taskId)
        firestore.collection(ObserverDbSchema.OBSERVER_TABLE).add(observer)
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

    fun updateTaskObservers(
        taskId: String,
        observers: List<AddObserverDto>
    ) {
        firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
            .whereEqualTo(ObserverDbSchema.TASK_ID, taskId).get()
            .continueWith { it.result.documents.forEach { it.reference.delete() } }.continueWith {
                observers.forEach {
                    if (it.isObserver) {
                        val observer = Observer(it.userId, it.isExecutor, taskId)
                        firestore.collection(ObserverDbSchema.OBSERVER_TABLE).add(observer)
                    }
                }
            }
    }

    fun getSubtasks(taskId: String): Flow<List<Task>> {
        return firestore.collection(TaskDbSchema.TASK_TABLE)
            .whereEqualTo(TaskDbSchema.PARENT_ID, taskId).snapshots().map {
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

    fun addComment(
        userId: String,
        comment: String,
        taskId: String,
        commentId: String
    ): GoogleTask<Void> {
        val data =
            mapOf<String, Any>(
                CommentDbSchema.USER_ID to userId,
                CommentDbSchema.TEXT to comment,
                CommentDbSchema.CREATED_AT to LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneOffset.UTC).toEpochSecond(),
                CommentDbSchema.TASK_ID to taskId
            )
        return firestore.collection(CommentDbSchema.COMMENT_TABLE).document(commentId).set(data)
    }

    fun changeTaskCompleted(task: Task, isCompleted: Boolean, completedById: String) {
        val today = LocalDate.now().toEpochDay()
        if (isCompleted) {
            val data = mapOf<String, Any?>(
                TaskDbSchema.LAST_COMPLETION_DATE to today,
                TaskDbSchema.PREVIOUS_COMPLETION_DATE to task.lastCompletionDate
            )
            firestore.collection(TaskDbSchema.TASK_TABLE).document(task.id).update(data)
                .continueWith {
                    firestore.collection(TaskDbSchema.TASK_TABLE)
                        .whereEqualTo(TaskDbSchema.PARENT_ID, task.id).get().continueWith {
                            for (subtask in it.result.documents) {
                                if (subtask.getLong(TaskDbSchema.LAST_COMPLETION_DATE) == today || !isTaskForToday(
                                        subtask.toObject(Task::class.java)!!,
                                        today
                                    )
                                ) {
                                    continue
                                }
                                subtask.reference.update(data)
                                val completion = mapOf<String, Any>(
                                    CompletionDbSchema.USER_ID to completedById,
                                    CompletionDbSchema.TASK_ID to subtask.id,
                                    CompletionDbSchema.COMPLETION_DATE to today
                                )
                                firestore.collection(CompletionDbSchema.COMPLETION_TABLE)
                                    .add(completion)
                            }
                        }
                }
                .continueWith {
                    val completion = mapOf<String, Any>(
                        CompletionDbSchema.USER_ID to completedById,
                        CompletionDbSchema.TASK_ID to task.id,
                        CompletionDbSchema.COMPLETION_DATE to today
                    )
                    firestore.collection(CompletionDbSchema.COMPLETION_TABLE).add(completion)
                }
        } else {
            val data = mapOf<String, Any?>(
                TaskDbSchema.PREVIOUS_COMPLETION_DATE to null,
                TaskDbSchema.LAST_COMPLETION_DATE to task.previousCompletionDate
            )
            firestore.collection(TaskDbSchema.TASK_TABLE).document(task.id).update(data)
                .continueWith {
                    firestore.collection(CompletionDbSchema.COMPLETION_TABLE)
                        .whereEqualTo(CompletionDbSchema.TASK_ID, task.id)
                        .whereEqualTo(CompletionDbSchema.COMPLETION_DATE, today).get()
                        .continueWith {
                            it.result.documents.forEach { it.reference.delete() }
                        }
                }
        }
    }

    fun deleteTask(taskId: String) {
        firestore.collection(TaskDbSchema.TASK_TABLE).document(taskId).delete()
            .continueWith {
                scope.launch {
                    firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
                        .whereEqualTo(ObserverDbSchema.TASK_ID, taskId).get().continueWith {
                            it.result.documents.forEach { it.reference.delete() }
                        }
                    firestore.collection(CompletionDbSchema.COMPLETION_TABLE)
                        .whereEqualTo(CompletionDbSchema.TASK_ID, taskId).get().continueWith {
                            it.result.documents.forEach { it.reference.delete() }
                        }
                    firestore.collection(CommentDbSchema.COMMENT_TABLE)
                        .whereEqualTo(CommentDbSchema.TASK_ID, taskId).get().continueWith {
                            it.result.documents.forEach {
                                it.reference.delete()
                                storage.reference.child("comment-${it.id}").delete()
                            }
                        }
                    storage.reference.child("task-$taskId").delete()
                }
            }
    }

    fun getObserver(taskId: String, userId: String): Flow<Observer?> {
        return firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
            .whereEqualTo(ObserverDbSchema.USER_ID, userId)
            .whereEqualTo(ObserverDbSchema.TASK_ID, taskId).limit(1).snapshots().map {
                it.documents.firstOrNull()?.toObject(Observer::class.java)
            }
    }

    suspend fun changeExecutorStatus(userId: String, taskId: String, isExecutor: Boolean) {
        firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
            .whereEqualTo(ObserverDbSchema.USER_ID, userId)
            .whereEqualTo(ObserverDbSchema.TASK_ID, taskId).get().await()
            .forEach { it.reference.update(ObserverDbSchema.EXECUTOR, isExecutor) }
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
        newValues[TaskDbSchema.TITLE] = taskData.title
        newValues[TaskDbSchema.DEADLINE] = taskData.deadline
        newValues[TaskDbSchema.IS_CONTINUOUS] = taskData.isContinuous
        newValues[TaskDbSchema.START_TIME] = taskData.startTime
        newValues[TaskDbSchema.FINISH_TIME] = taskData.finishTime
        newValues[TaskDbSchema.REPEAT_TYPE] = taskData.repeatType
        newValues[TaskDbSchema.N_DAYS] = taskData.nDays
        newValues[TaskDbSchema.DAYS_OF_WEEK] = taskData.daysOfWeek
        newValues[TaskDbSchema.REPEAT_START] = taskData.repeatStart
        newValues[TaskDbSchema.IMPORTANCE] = taskData.importance
        newValues[TaskDbSchema.LOCATION] = taskData.location
        newValues[TaskDbSchema.ADDRESS] = taskData.address
        firestore.collection(TaskDbSchema.TASK_TABLE).document(taskId).update(newValues)
    }

    fun removeTasksForUser(userId: String) {
        firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
            .whereEqualTo(ObserverDbSchema.USER_ID, userId).get().continueWith {
                it.result.documents.forEach { it.reference.delete() }
            }
        firestore.collection(CompletionDbSchema.COMPLETION_TABLE)
            .whereEqualTo(CompletionDbSchema.USER_ID, userId).get().continueWith {
                it.result.documents.forEach { it.reference.delete() }
            }
        firestore.collection(CommentDbSchema.COMMENT_TABLE)
            .whereEqualTo(CommentDbSchema.USER_ID, userId).get().continueWith {
                it.result.documents.forEach {
                    it.reference.delete()
                    storage.reference.child("comment-${it.id}").delete()
                }
            }
    }

    fun removeTasksForFamily(familyId: String) {
        firestore.collection(TaskDbSchema.TASK_TABLE)
            .whereEqualTo(TaskDbSchema.FAMILY_ID, familyId).get().continueWith {
                val taskIds = it.result.documents.map { it.id }
                var i = 0
                while (i < taskIds.size) {
                    firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
                        .whereIn(ObserverDbSchema.TASK_ID, taskIds.subList(i, i + 30)).get()
                        .continueWith {
                            it.result.documents.forEach { it.reference.delete() }
                        }
                    i += 30
                }
                taskIds.forEach { deleteTask(it) }
            }
    }

    private fun isTaskForToday(
        task: Task,
        today: Long,
    ): Boolean {
        return when (task.repeatType) {
            RepeatType.ONCE -> {
                (task.deadline != null && task.deadline!! <= today || task.deadline == null) && (task.lastCompletionDate == today || task.lastCompletionDate == null)
            }

            RepeatType.EVERY_DAY -> {
                task.repeatStart != null && task.repeatStart!! <= today
            }

            RepeatType.DAYS_OF_WEEK -> {
                val startDate =
                    if (task.lastCompletionDate != null) task.lastCompletionDate!! + 1 else task.repeatStart
                var isForToday = false
                for (i in startDate!!..today) {
                    if (task.daysOfWeek and (2.0.pow(LocalDate.ofEpochDay(today).dayOfWeek.value - 1))
                            .toInt() > 0
                    ) {
                        isForToday = true
                        break
                    }
                }
                isForToday
            }

            RepeatType.EACH_N_DAYS -> {
                task.lastCompletionDate == null || task.lastCompletionDate!! + task.nDays <= today
            }
        }
    }
}