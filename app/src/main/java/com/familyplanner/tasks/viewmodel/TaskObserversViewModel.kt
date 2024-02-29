package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.common.User
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TaskObserversViewModel : ViewModel() {
    private var members: MutableSharedFlow<List<User>> = MutableSharedFlow<List<User>>(replay = 1)
    private val familyRepo = FamilyRepository()
    private val tasksRepo = TaskRepository()

    fun getMembers(): Flow<List<User>> = members

    fun setFamily(familyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            familyRepo.getFamilyMembers(familyId).collect {
                members.emit(it)
            }
        }
    }

    fun setObserversAndExecutors(members: List<User>, observers: BooleanArray, executors: BooleanArray, taskId: String) {
        tasksRepo.addTaskObservers(taskId, members, observers, executors)
    }
}