package com.familyplanner.tasks.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.tasks.dto.CommentDto
import com.familyplanner.tasks.dto.ObserverDto
import com.familyplanner.tasks.model.Comment
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.Status
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.repository.TaskRepository
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.ToponymObjectMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.net.URL

class TaskInfoViewModel : ViewModel() {
    private var task: MutableSharedFlow<Task?> = MutableSharedFlow(replay = 1)
    private var comments: MutableSharedFlow<List<CommentDto>> = MutableSharedFlow(replay = 1)
    private var observers: MutableSharedFlow<List<ObserverDto>> = MutableSharedFlow(replay = 1)
    private var executors: MutableSharedFlow<List<Observer>> = MutableSharedFlow(replay = 1)
    private val repo = TaskRepository()
    private var subTasks: MutableSharedFlow<List<Task>> = MutableSharedFlow(replay = 1)
    private var files = MutableSharedFlow<List<String>?>(replay = 1)

    private val searchSessionListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val items = response.collection.children.mapNotNull {
                it.obj?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)?.address?.formattedAddress
                    ?: it.obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)?.address?.formattedAddress
            }

            curAddress = items[0]
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

    fun setTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getTaskById(taskId).collect {
                task.emit(it)
            }

            repo.getTaskComments(taskId).collect {
                comments.emit(it)
            }

            repo.getTaskObservers(taskId).collect {
                executors.emit(it)
            }

            repo.getSubtasks(taskId).collect {
                subTasks.emit(it)
            }

            repo.getFilesForTask(taskId).addOnCompleteListener {
                val curFiles = if (it.isSuccessful) {
                    it.result.prefixes.map { it.path }
                } else {
                    null
                }
                viewModelScope.launch(Dispatchers.IO) {
                    files.emit(curFiles)
                }
            }
        }
    }

    fun getTask(): Flow<Task?> = task

    fun getComments(): Flow<List<CommentDto>> = comments

    fun getObservers(): Flow<List<ObserverDto>> = observers

    fun getSubtasks(): Flow<List<Task>> = subTasks

    fun getFiles(): Flow<List<String>?> = files

    fun downloadFile(path: String): Uri {
        return runBlocking { repo.downloadFile(path).await() }
    }

    fun changeCompleted(taskId: String, isCompleted: Boolean, completedById: String) {
        repo.changeTaskCompleted(taskId, isCompleted, completedById)
    }

    fun addComment(userId: String, comment: String) {
        repo.addComment(userId, comment)
    }
}
