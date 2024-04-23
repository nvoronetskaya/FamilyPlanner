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
import com.familyplanner.tasks.model.Status
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.TaskCreationStatus
import com.familyplanner.tasks.model.UserFile
import com.familyplanner.tasks.repository.TaskRepository
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.ToponymObjectMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.math.pow

class TaskInfoViewModel : ViewModel() {
    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val searchStatus = MutableSharedFlow<Status>()
    private var curAddress = MutableSharedFlow<String>()
    private var task: MutableSharedFlow<Task?> = MutableSharedFlow(replay = 1)
    private var comments: MutableSharedFlow<List<CommentDto>> = MutableSharedFlow(replay = 1)
    private var observers: MutableSharedFlow<List<ObserverDto>> = MutableSharedFlow(replay = 1)
    private val repo = TaskRepository()
    private var subTasks: MutableSharedFlow<List<TaskWithDate>> = MutableSharedFlow(replay = 1)
    private var files = MutableSharedFlow<List<String>?>(replay = 1)
    private var taskId: String = ""
    private val addComment = MutableSharedFlow<TaskCreationStatus>()
    private val curObserver = MutableSharedFlow<Observer?>(replay = 1)

    private val searchSessionListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val items = response.collection.children.mapNotNull {
                it.obj?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)?.address?.formattedAddress
                    ?: it.obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)?.address?.formattedAddress
            }

            viewModelScope.launch(Dispatchers.IO) {
                curAddress.emit(items[0])
            }
            viewModelScope.launch(Dispatchers.IO) {
                searchStatus.emit(Status.SUCCESS)
            }
        }

        override fun onSearchError(p0: com.yandex.runtime.Error) {
            viewModelScope.launch(Dispatchers.IO) {
                searchStatus.emit(Status.ERROR)
            }
        }
    }

    fun getAddressByGeo(obj: Point): Flow<Status> {
        viewModelScope.launch(Dispatchers.Main) {
            searchManager.submit(obj, 21, SearchOptions(), searchSessionListener)
        }
        return searchStatus
    }

    fun getAddress(): Flow<String> = curAddress

    fun setTask(taskId: String) {
        if (taskId == this.taskId) {
            return
        }
        this.taskId = taskId
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                repo.getTaskById(taskId).collect {
                    getAddressByGeo(
                        Point(
                            it?.location?.latitude ?: 0.0,
                            it?.location?.longitude ?: 0.0
                        )
                    )
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

    fun downloadFile(taskId: String, path: String): Uri {
        return runBlocking { repo.downloadFile("$taskId/$path").await() }
    }

    fun changeCompleted(taskId: String, isCompleted: Boolean, completedById: String) {
        repo.changeTaskCompleted(taskId, isCompleted, completedById)
    }

    fun addComment(userId: String, comment: String, files: List<UserFile>) {
        repo.addComment(userId, comment, taskId).addOnCompleteListener {
            viewModelScope.launch(Dispatchers.IO) {
                val result: TaskCreationStatus
                if (!it.isSuccessful) {
                    result = TaskCreationStatus.FAILED
                } else {
                    val createdCommentId = it.result.id
                    result = if (files.isNotEmpty()) {
                        if (!repo.tryUploadFiles(files, createdCommentId, "comment")) {
                            TaskCreationStatus.FILE_UPLOAD_FAILED
                        } else {
                            TaskCreationStatus.SUCCESS
                        }
                    } else {
                        TaskCreationStatus.SUCCESS
                    }
                }
                addComment.emit(result)
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

    fun getTasksWithDate(tasks: List<Task>): List<TaskWithDate> {
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
