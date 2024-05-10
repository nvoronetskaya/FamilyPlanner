package com.familyplanner.family.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.data.User
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.family.data.Family
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class MembersListViewModel : ViewModel() {
    private val userId: String = FamilyPlanner.userId
    private var familyId = ""
    private val errors = MutableSharedFlow<String>()
    private val repository = FamilyRepository()
    private var family = MutableSharedFlow<Family?>(replay = 1)
    private var members = MutableSharedFlow<List<User>>(replay = 1)
    private var applicants = MutableSharedFlow<List<User>>(replay = 1)

    fun getErrors(): Flow<String> = errors

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getUserById(userId).collect { user ->
                this@MembersListViewModel.familyId = user.familyId ?: ""

                if (familyId.isNullOrBlank()) {
                    family.emit(null)
                } else {
                    launch {
                        repository.getFamilyById(familyId).collect {
                            family.emit(it)
                        }
                    }
                    launch {
                        repository.getFamilyMembers(familyId).collect {
                            members.emit(it)
                        }
                    }
                    launch {
                        repository.getApplicationsToFamily(familyId).collect {
                            launch {
                                repository.getApplicants(it.map { application -> application.userId })
                                    .collect { users ->
                                        applicants.emit(users)
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateFamilyName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateFamily(familyId, newName)
            } catch (e: Exception) {
                errors.emit("Не удалось изменить название, попробуйте позднее")
            }
        }
    }

    fun getFamily(): Flow<Family?> = family

    fun remove(userId: String) {
        repository.removeMember(userId)
    }

    fun getMembers(): Flow<List<User>> = members

    fun leave(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            family.emit(null)
        }
        remove(userId)
    }

    fun deleteFamily(): Flow<Boolean> {
        val isSuccessful = MutableSharedFlow<Boolean>()

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFamily(family.last()!!.id).addOnCompleteListener {
                viewModelScope.launch (Dispatchers.IO) {
                    if (it.isSuccessful) {
                        isSuccessful.emit(true)
                    } else {
                        isSuccessful.emit(false)
                    }
                }
            }
        }
        return isSuccessful
    }

    fun getApplicants(): Flow<List<User>> = applicants

    fun approve(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.approveApplication(userId, familyId)
            } catch (e: Exception) {
                errors.emit("Не удалось одобрить заявку, попробуйте позднее")
            }
        }
    }

    fun reject(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.rejectApplication(userId, familyId)
            } catch (e: Exception) {
                errors.emit("Не удалось отменить заявку, попробуйте позднее")
            }
        }
    }
}