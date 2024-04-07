package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.tasks.model.Importance
import com.familyplanner.tasks.model.RepeatType
import com.familyplanner.tasks.model.Status
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.model.TaskCreationStatus
import com.familyplanner.tasks.model.UserFile
import com.familyplanner.tasks.repository.TaskRepository
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.UploadTask
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
import java.time.LocalDate
import java.util.Date

class NewTaskViewModel : ViewModel() {
    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val session = searchManager.createSuggestSession()
    private val searchStatus = MutableSharedFlow<Status>()
    private val addTask = MutableSharedFlow<TaskCreationStatus>()
    private var curAddress = ""
    private val familyRepo = FamilyRepository()
    private val tasksRepo = TaskRepository()
    private var familyId = ""
    private var uploadTasks = listOf<UploadTask>()
    private var createdTaskId: String? = null

    fun setFamilyId(familyId: String) {
        this.familyId = familyId
    }

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
        hasDeadline: Boolean,
        deadline: Long?,
        isContinuous: Boolean,
        startTime: Int,
        finishTime: Int,
        repeatType: RepeatType,
        nDays: Int,
        daysOfWeek: Int,
        repeatStart: Long?,
        importance: Importance,
        hasLocation: Boolean,
        location: Point?,
        userId: String,
        familyId: String,
        files: List<UserFile>?,
        parentId: String?
    ) {
        val newTask = Task()
        newTask.title = title
        newTask.hasDeadline = hasDeadline
        newTask.deadline = deadline
        newTask.isContinuous = isContinuous
        newTask.startTime = startTime
        newTask.finishTime = finishTime
        newTask.repeatType = repeatType
        newTask.nDays = nDays
        newTask.daysOfWeek = daysOfWeek
        newTask.repeatStart = repeatStart
        newTask.importance = importance
        newTask.hasLocation = hasLocation
        newTask.location =
            if (location != null) GeoPoint(location.latitude, location.longitude) else null
        newTask.createdBy = userId
        newTask.familyId = familyId
        newTask.parentId = parentId

        tasksRepo.addTask(newTask).addOnCompleteListener {
            viewModelScope.launch(Dispatchers.IO) {
                var result: TaskCreationStatus
                if (!it.isSuccessful) {
                    result = TaskCreationStatus.FAILED
                } else {
                    createdTaskId = it.result.id
                    tasksRepo.addCreatorObserver(createdTaskId!!, userId)
                    if (files != null) {
                        result = if (!tasksRepo.tryUploadFiles(files, it.result.id)) {
                            TaskCreationStatus.FILE_UPLOAD_FAILED
                        } else {
                            TaskCreationStatus.SUCCESS
                        }
                    } else {
                        result = TaskCreationStatus.SUCCESS
                    }
                }
                addTask.emit(result)
            }
        }
    }

    fun getAddress() = curAddress
    fun getCreationStatus() = addTask
    fun getCreatedTaskId() = createdTaskId
}