package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.repository.UserRepository
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.tasks.data.AddObserverDto
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditTaskObserversViewModel : ViewModel() {
    private val observers = mutableMapOf<String, AddObserverDto>()
    private val familyRepo = FamilyRepository()
    private val userRepo = UserRepository()
    private val taskRepo = TaskRepository()
    private var familyId = ""

    suspend fun getObservers(
        taskId: String,
        requestDataUpdate: Boolean = false
    ): List<AddObserverDto> {
        if (observers.isEmpty() || requestDataUpdate) {
            familyId = userRepo.getUserByIdOnce(FamilyPlanner.userId).familyId ?: return listOf()
            val members = familyRepo.getFamilyMembersOnce(familyId)
            members.forEach {
                observers[it.id] = AddObserverDto(it.id, it.name, it.birthday, false, false)
            }
            val curObservers = taskRepo.getTaskObserversOnce(taskId)
            curObservers.forEach {
                if (observers.contains(it.userId)) {
                    observers[it.userId]!!.isObserver = true
                    observers[it.userId]!!.isExecutor = it.isExecutor
                }
            }
        }
        return observers.values.toList()
    }

    fun updateObservers(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepo.updateTaskObservers(taskId, observers.values.toList())
        }
    }
}