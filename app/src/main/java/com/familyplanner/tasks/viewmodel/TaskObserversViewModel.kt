package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.tasks.data.AddObserverDto
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskObserversViewModel : ViewModel() {
    private val observers = mutableListOf<AddObserverDto>()
    private val familyRepo = FamilyRepository()
    private val tasksRepo = TaskRepository()
    private var familyId: String = ""

    suspend fun getObservers(familyId: String): List<AddObserverDto> {
        this.familyId = familyId
        if (observers.isEmpty()) {
            val members = familyRepo.getFamilyMembersOnce(familyId)
            members.forEach { observers.add(AddObserverDto(it.id, it.name, it.birthday, false, false)) }
        }
        return observers
    }

    fun setObserversAndExecutors(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tasksRepo.updateTaskObservers(taskId, observers)
        }
    }
}