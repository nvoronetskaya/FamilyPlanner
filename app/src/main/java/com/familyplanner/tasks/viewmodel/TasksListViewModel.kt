package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.common.repository.UserRepository
import com.familyplanner.common.data.User
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.tasks.data.TaskWithDate
import com.familyplanner.tasks.data.Importance
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.SortingType
import com.familyplanner.tasks.data.Task
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.pow

class TasksListViewModel : ViewModel() {
    private val filteredTasks = MutableSharedFlow<List<TaskWithDate>>(replay = 1)
    private val allTasks = mutableListOf<TaskWithDate>()
    private var userId: String = ""
    private val users = mutableListOf<User>()
    private val userRepo = UserRepository()
    private val familyRepo = FamilyRepository()
    private val taskRepo = TaskRepository()
    private var sortingType = SortingType.NONE
    private var hasLocationFilter: Boolean? = null
    private var hasDeadlineFilter: Boolean? = null
    private val userFilter = MutableSharedFlow<String>(replay = 1)
    private var importanceFilter: Importance? = null
    private var userFilterId: String? = null
    private var curDate: LocalDate = LocalDate.now()
    private var familyId: String? = null
    private var adminId: String? = null

    fun setUser(userId: String) {
        if (userId == this.userId) {
            return
        }
        this.userId = userId
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                userFilter.emit(userId)
                userRepo.getUserById(userId).collect {
                    familyId = it.familyId
                    if (familyId.isNullOrEmpty()) {
                        return@collect
                    }
                    adminId = familyRepo.getFamilyByIdOnce(familyId!!)?.createdBy
                    familyRepo.getFamilyMembers(familyId!!).collect { members ->
                        users.clear()
                        users.addAll(members)
                    }
                }
            }
            launch {
                userFilter.collect {
                    launch {
                        taskRepo.getSharedTasks(userId, it).collect { tasks ->
                            allTasks.clear()
                            allTasks.addAll(tasks.map { task -> TaskWithDate(task, null) })
                            filteredTasks.emit(getTasksForDate(curDate, applyFilters()))
                        }
                    }
                }
            }
        }
    }

    fun getFamilyId(): String? = familyId

    fun setUserFilter(filterUserId: String?) {
        userFilterId = filterUserId
    }

    fun setImportanceFilter(importance: Importance?) {
        importanceFilter = importance
    }

    fun setLocationFilter(hasDeadline: Boolean?) {
        hasDeadlineFilter = hasDeadline
    }

    fun setDeadlineFilter(hasDeadline: Boolean?) {
        hasDeadlineFilter = hasDeadline
    }

    fun getTasks(): Flow<List<TaskWithDate>> = filteredTasks

    fun setSortingType(type: SortingType) {
        sortingType = type
    }

    fun startFilterUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            userFilter.emit(userFilterId ?: userId)
        }
    }

    fun updateDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            filteredTasks.emit(getTasksForDate(date, applyFilters()))
        }
    }

    private fun getTasksForDate(
        date: LocalDate,
        curAllTasks: List<TaskWithDate>? = filteredTasks.replayCache.lastOrNull()
    ): List<TaskWithDate> {
        curAllTasks ?: return listOf()
        this.curDate = date
        val dateEpochDay = date.toEpochDay()
        val today = LocalDate.now().toEpochDay()
        val tasksForDate = mutableListOf<TaskWithDate>()
        for (task in curAllTasks) {
            if (task.task.lastCompletionDate == today) {
                task.date = null
                tasksForDate.add(task)
                continue
            }
            when (task.task.repeatType) {
                RepeatType.ONCE -> {
                    val shouldDoToday =
                        (task.task.deadline != null && task.task.deadline!! <= dateEpochDay || task.task.deadline == null) && (task.task.lastCompletionDate == today || task.task.lastCompletionDate == null)
                    if (task.task.deadline != null && task.task.deadline!! == dateEpochDay || shouldDoToday && today == dateEpochDay) {
                        task.date = if (task.task.deadline != null) task.task.deadline else null
                        tasksForDate.add(task)
                    }
                }

                RepeatType.EVERY_DAY -> {
                    if (task.task.repeatStart != null && task.task.repeatStart!! <= dateEpochDay) {
                        task.date =
                            if (task.task.lastCompletionDate == null) task.task.repeatStart else task.task.lastCompletionDate!! + 1
                        tasksForDate.add(task)
                    }
                }

                RepeatType.DAYS_OF_WEEK -> {
                    if (dateEpochDay == today) {
                        val startDate =
                            if (task.task.lastCompletionDate != null) task.task.lastCompletionDate!! + 1 else task.task.repeatStart
                        for (i in startDate!!..dateEpochDay) {
                            if (task.task.daysOfWeek and 2.0.pow(date.dayOfWeek.value)
                                    .toInt() > 0
                            ) {
                                task.date = i
                                break
                            }
                        }
                        tasksForDate.add(task)
                    } else if (task.task.repeatStart!! <= dateEpochDay && (task.task.daysOfWeek and 2.0.pow(
                            date.dayOfWeek.value
                        ).toInt() > 0)
                    ) {
                        tasksForDate.add(task)
                    }
                }

                RepeatType.EACH_N_DAYS -> {
                    val shouldDoToday =
                        today == dateEpochDay && (task.task.lastCompletionDate == null || task.task.lastCompletionDate!! + task.task.nDays <= dateEpochDay)
                    if (task.task.repeatStart!! <= dateEpochDay && (((dateEpochDay - task.task.repeatStart!!) % task.task.nDays).toInt() == 0 || shouldDoToday)) {
                        task.date = if (task.task.lastCompletionDate == null) {
                            task.task.repeatStart
                        } else {
                            task.task.repeatStart!! + ((task.task.lastCompletionDate!! - task.task.repeatStart!!) / task.task.nDays + 1) * task.task.nDays
                        }
                        tasksForDate.add(task)
                    }
                }
            }
            if (dateEpochDay != today) {
                tasksForDate.lastOrNull()?.date = null
            }
        }
        return tasksForDate
    }

    fun getUsers(): List<User> = users

    fun getUserFilter(): String? = userFilterId

    fun getImportanceFilter(): Importance? = importanceFilter

    fun getLocationFilter(): Boolean? = hasLocationFilter

    fun getDeadlineFilter(): Boolean? = hasDeadlineFilter

    fun getSortingType(): SortingType = sortingType

    private fun applyFilters(): List<TaskWithDate> {
        val result =
            List(allTasks.size) { i -> allTasks[i] }.filter { if (hasLocationFilter != null) (it.task.location != null) == hasLocationFilter else true }
                .filter { if (hasDeadlineFilter != null) (it.task.deadline != null) == hasDeadlineFilter else true }
                .filter { if (importanceFilter != null) it.task.importance == importanceFilter else true }
        return when (sortingType) {
            SortingType.NONE -> result
            SortingType.IMPORTANCE_ASC -> result.sortedBy { it.task.importance }
            SortingType.IMPORTANCE_DESC -> result.sortedByDescending { it.task.importance }
            SortingType.DEADLINE_ASC -> result.sortedBy { it.task.deadline }
            SortingType.DEADLINE_DESC -> result.sortedByDescending { it.task.deadline }
        }
    }

    fun changeCompleted(task: Task, isCompleted: Boolean, completedById: String) {
        taskRepo.changeTaskCompleted(task, isCompleted, completedById)
    }

    fun isAdmin() = adminId.equals(userId)
}