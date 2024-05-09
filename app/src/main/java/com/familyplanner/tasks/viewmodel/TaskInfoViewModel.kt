package com.familyplanner.tasks.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.tasks.dto.CommentDto
import com.familyplanner.tasks.dto.ObserverDto
import com.familyplanner.tasks.dto.TaskWithDate
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.RepeatType
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.TaskCreationStatus
import com.familyplanner.tasks.model.UserFile
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.math.pow

class TaskInfoViewModel : ViewModel() {
    private var task: MutableSharedFlow<Task?> = MutableSharedFlow(replay = 1)
    private var comments: MutableSharedFlow<List<CommentDto>> = MutableSharedFlow(replay = 1)
    private var observers: MutableSharedFlow<List<ObserverDto>> = MutableSharedFlow(replay = 1)
    private val repo = TaskRepository()
    private var subTasks: MutableSharedFlow<List<TaskWithDate>> = MutableSharedFlow(replay = 1)
    private var files = MutableSharedFlow<List<String>?>(replay = 1)
    private var taskId: String = ""
    private val addComment = MutableSharedFlow<TaskCreationStatus>()
    private val curObserver = MutableSharedFlow<Observer?>(replay = 1)

    fun setTask(taskId: String) {
        if (taskId == this.taskId) {
            return
        }
        this.taskId = taskId
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                repo.getTaskById(taskId).collect {
                    task.emit(it)
                }
            }
            launch {
                repo.getTaskComments(taskId).collect {
                    comments.emit(it)
                }
            }
            launch {
                repo.getTaskObservers(taskId).collect {
                    observers.emit(it)
                }
            }
            launch {
                repo.getSubtasks(taskId).collect {
                    subTasks.emit(getTasksWithDate(it))
                }
            }
            launch {
                repo.getFilesForTask(taskId).addOnCompleteListener {
                    val curFiles = if (it.isSuccessful) {
                        it.result.items.map { it.name }
                    } else {
                        null
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        files.emit(curFiles)
                    }
                }
            }
            launch {
                repo.getObserver(taskId, FamilyPlanner.userId).collect {
                    curObserver.emit(it)
                }
            }
        }
    }

    fun getTask(): Flow<Task?> = task

    fun getComments(): Flow<List<CommentDto>> = comments

    fun getObservers(): Flow<List<ObserverDto>> = observers

    fun getSubtasks(): Flow<List<TaskWithDate>> = subTasks

    fun getFiles(): Flow<List<String>?> = files

    fun downloadFile(prefix: String, taskId: String, path: String): Uri {
        return runBlocking { repo.downloadFile("$prefix-$taskId/$path").await() }
    }

    fun changeCompleted(task: Task, isCompleted: Boolean, completedById: String) {
        repo.changeTaskCompleted(task, isCompleted, completedById)
    }

    fun addComment(userId: String, comment: String, files: List<UserFile>) {
        repo.addComment(userId, comment, taskId).addOnCompleteListener {
            viewModelScope.launch(Dispatchers.IO) {
//                val result: TaskCreationStatus = TaskCreationStatus.SUCCESS
//                if (!it.isSuccessful) {
//                    result = TaskCreationStatus.FAILED
//                } else {
//                    val createdCommentId = it.result.id
//                    result = if (files.isNotEmpty()) {
//                        if (!repo.tryUploadFiles(files, createdCommentId, "comment")) {
//                            TaskCreationStatus.FILE_UPLOAD_FAILED
//                        } else {
//                            TaskCreationStatus.SUCCESS
//                        }
//                    } else {
//                        TaskCreationStatus.SUCCESS
//                    }
//                }
//                addComment.emit(result)
            }
        }
    }

    fun getCreationStatus(): Flow<TaskCreationStatus> = addComment

    fun getCurObserver(): Flow<Observer?> = curObserver

    fun changeExecutorStatus(userId: String, taskId: String, isExecutor: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.changeExecutorStatus(userId, taskId, isExecutor)
        }
    }

    private fun getTasksWithDate(tasks: List<Task>): List<TaskWithDate> {
        val tasksForDate = mutableListOf<TaskWithDate>()
        for (task in tasks) {
            val taskWithDate = TaskWithDate(task, null)
            when (task.repeatType) {
                RepeatType.ONCE -> taskWithDate.date = task.deadline

                RepeatType.EVERY_DAY -> taskWithDate.date =
                    if (task.lastCompletionDate == null) task.repeatStart else task.lastCompletionDate!! + 1

                RepeatType.DAYS_OF_WEEK -> {
                    var startDate =
                        if (task.lastCompletionDate != null) task.lastCompletionDate!! + 1 else task.repeatStart

                    while (true) {
                        if (task.daysOfWeek and 2.0.pow(LocalDate.ofEpochDay(startDate!!).dayOfWeek.value)
                                .toInt() > 0
                        ) {
                            taskWithDate.date = startDate
                            break
                        }
                        ++startDate
                    }
                }

                RepeatType.EACH_N_DAYS -> {
                    taskWithDate.date = if (task.lastCompletionDate == null) {
                        task.repeatStart
                    } else {
                        task.repeatStart!! + ((task.lastCompletionDate!! - task.repeatStart!!) / task.nDays + 1) * task.nDays
                    }
                }
            }
            tasksForDate.add(taskWithDate)

        }
        return tasksForDate
    }

    fun deleteTask(taskId: String) {
        repo.deleteTask(taskId)
    }
}
