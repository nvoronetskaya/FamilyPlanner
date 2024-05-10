package com.familyplanner.tasks.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.tasks.data.Importance
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.Status
import com.familyplanner.tasks.data.Task
import com.familyplanner.tasks.data.UserFile
import com.familyplanner.tasks.repository.TaskRepository
import com.google.firebase.firestore.GeoPoint
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
import kotlinx.coroutines.tasks.await

class EditTaskViewModel : ViewModel() {
    private var files = mutableListOf<UserFile>()
    private var taskId = ""
    private var task: Task? = null
    private val taskRepo = TaskRepository()
    private var curAddress = ""
    private val searchStatus = MutableSharedFlow<Status>()
    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var isFileUploadSuccessful: Boolean = true

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

    fun getAddress() = curAddress

    suspend fun getTask(taskId: String): Task? {
        if (taskId != this.taskId) {
            this.taskId = taskId
            task = taskRepo.getTaskByIdOnce(taskId)
            task?.location?.let { getAddressByGeo(Point(it.latitude, it.longitude)) }
            files.clear()
            files.addAll(taskRepo.getFilesForTask(taskId).await().items.map {
                UserFile(
                    Uri.EMPTY,
                    it.name,
                    0.0
                )
            })
        }
        return task
    }

    fun removeFile(fileName: String) {
        taskRepo.removeFile(taskId, fileName)
    }

    fun addFile(file: UserFile) {
        viewModelScope.launch {
            if (!taskRepo.addFile(taskId, file)) {
                isFileUploadSuccessful = false
            }
        }
    }

    fun getIsFilesUploadSuccessful() = isFileUploadSuccessful

    fun getFiles(): List<UserFile> = files

    fun updateTask(
        title: String,
        deadline: Long?,
        isContinuous: Boolean,
        startTime: Int,
        finishTime: Int,
        repeatType: RepeatType,
        nDays: Int,
        daysOfWeek: Int,
        repeatStart: Long?,
        importance: Importance,
        location: Point?,
        address: String?
    ) {
        val newTask = Task()
        newTask.title = title
        newTask.deadline = deadline
        newTask.isContinuous = isContinuous
        newTask.startTime = startTime
        newTask.finishTime = finishTime
        newTask.repeatType = repeatType
        newTask.nDays = nDays
        newTask.daysOfWeek = daysOfWeek
        newTask.repeatStart = repeatStart
        newTask.importance = importance
        newTask.location =
            if (location != null) GeoPoint(location.latitude, location.longitude) else null
        newTask.address = address

        taskRepo.updateTask(taskId, newTask)
    }
}