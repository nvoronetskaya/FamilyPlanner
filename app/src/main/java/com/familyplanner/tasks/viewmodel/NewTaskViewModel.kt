package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.tasks.model.Importance
import com.familyplanner.tasks.model.RepeatType
import com.familyplanner.tasks.model.Status
import com.familyplanner.tasks.model.Task
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
import java.util.Date

class NewTaskViewModel : ViewModel() {
    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val session = searchManager.createSuggestSession()
    private val searchStatus = MutableSharedFlow<Status>()
    private var curAddress = ""
    private val familyRepo = FamilyRepository()
    private var familyId = ""

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
        deadline: Date,
        isContinuous: Boolean,
        startTime: Int,
        finishTime: Int,
        repeatType: RepeatType,
        nDays: Int,
        daysOfWeek: Int,
        repeatStart: Date,
        importance: Importance,
        hasLocation: Boolean,
        location: Point,
        isPrivate: Boolean,
        userId: String,
        familyId: String
    ) {
        val newTask = Task()
        newTask.title = title
        newTask.hasDeadline = hasDeadline
        newTask.deadline = deadline.toString()
        newTask.isContinuous = isContinuous
        newTask.startTime = startTime
        newTask.finishTime = finishTime
        newTask.repeatType = repeatType
        newTask.nDays = nDays
        newTask.daysOfWeek = daysOfWeek
        newTask.repeatStart = repeatStart.toString()
        newTask.importance = importance
        newTask.hasLocation = hasLocation
        newTask.location = GeoPoint(location.latitude, location.longitude)
        newTask.isPrivate = isPrivate
        newTask.createdBy = userId
        newTask.familyId = familyId
    }

    fun getAddress() = curAddress
}