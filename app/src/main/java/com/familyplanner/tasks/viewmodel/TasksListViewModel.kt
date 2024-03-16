package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.common.User
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.tasks.model.Importance
import com.familyplanner.tasks.model.SortingType
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TasksListViewModel : ViewModel() {
    private val filteredTasks = MutableStateFlow<List<Task>>(listOf())
    private val allTasks = mutableListOf<Task>()
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userFilter.collect {
                taskRepo.getSharedTasks(userId, it).collect { tasks ->
                    allTasks.clear()
                    allTasks.addAll(tasks)
                    filteredTasks.emit(applyFilters())
                }
            }
        }
    }

    fun setUser(userId: String) {
        this.userId = userId
        viewModelScope.launch(Dispatchers.IO) {
            userFilter.emit(userId)
            userRepo.getUserById(userId).collect {
                familyRepo.getFamilyMembers(it.familyId ?: "").collect { members ->
                    users.clear()
                    users.addAll(members)
                }
            }
        }
    }

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

    fun getTasks(): Flow<List<Task>> = filteredTasks

    fun setSortingType(type: SortingType) {
        sortingType = type
    }

    fun startFilterUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            userFilter.emit(userFilterId ?: userId)
        }
    }

    fun getUsers(): List<User> = users

    fun getUserFilter(): String? = userFilterId

    fun getImportanceFilter(): Importance? = importanceFilter

    fun getLocationFilter(): Boolean? = hasLocationFilter

    fun getDeadlineFilter(): Boolean? = hasDeadlineFilter

    fun getSortingType(): SortingType = sortingType

    private fun applyFilters(): List<Task> {
        val result =
            allTasks.filter { if (hasLocationFilter != null) it.hasLocation == hasLocationFilter else true }
                .filter { if (hasDeadlineFilter != null) it.hasDeadline == hasDeadlineFilter else true }
                .filter { if (importanceFilter != null) it.importance == importanceFilter else true }
        return when (sortingType) {
            SortingType.NONE -> result
            SortingType.IMPORTANCE_ASC -> result.sortedBy { it.importance }
            SortingType.IMPORTANCE_DESC -> result.sortedByDescending { it.importance }
            SortingType.DEADLINE_ASC -> result.sortedBy { it.deadline }
            SortingType.DEADLINE_DESC -> result.sortedByDescending { it.deadline }
        }
    }
}