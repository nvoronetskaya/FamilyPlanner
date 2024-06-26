package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.tasks.data.Importance
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.Status
import com.familyplanner.tasks.data.Task
import com.familyplanner.tasks.data.TaskCreationStatus
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
import java.util.UUID

class NewTaskViewModel : ViewModel() {
    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val session = searchManager.createSuggestSession()
    private val searchStatus = MutableSharedFlow<Status>()
    private var curAddress = ""
    private val familyRepo = FamilyRepository()
    private val tasksRepo = TaskRepository()
    private var createdTaskId: String? = null
    private val addTask = MutableSharedFlow<TaskCreationStatus>()

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

    fun createTask(
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
        address: String?,
        userId: String,
        familyId: String,
        files: List<UserFile>?,
        parentId: String?,
        isConnected: Boolean
    ) {
        val newTask = Task()
        newTask.id = UUID.randomUUID().toString()
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
        newTask.createdBy = userId
        newTask.familyId = familyId
        newTask.parentId = parentId
        newTask.address = address

        viewModelScope.launch(Dispatchers.IO) {
            tasksRepo.addTask(newTask)
            tasksRepo.addCreatorObserver(newTask.id, userId)
            val result = if (!files.isNullOrEmpty()) {
                if (!isConnected || !tasksRepo.tryUploadFiles(files, newTask.id)) {
                    TaskCreationStatus.FILE_UPLOAD_FAILED
                } else {
                    TaskCreationStatus.SUCCESS
                }
            } else {
                TaskCreationStatus.SUCCESS
            }
            addTask.emit(result)
        }
        createdTaskId = newTask.id
    }

    fun getAddress() = curAddress
    fun getCreatedTaskId() = createdTaskId
    fun getCreationStatus(): Flow<TaskCreationStatus> = addTask
}