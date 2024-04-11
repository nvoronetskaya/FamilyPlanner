package com.familyplanner.family.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.User
import com.familyplanner.family.data.FamilyRepository
import com.familyplanner.family.model.Family
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class ApplicationListViewModel : ViewModel() {
    private val userId: String = FamilyPlanner.userId
    private val errors = MutableSharedFlow<String>()
    private val repository = FamilyRepository()
    private var family = MutableSharedFlow<Family?>()
    private var applicants = MutableSharedFlow<List<User>>()

    fun getErrors(): Flow<String> = errors

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getUserById(userId).collect { user ->
                val familyId = user.familyId

                if (familyId.isNullOrBlank()) {
                    family.emit(null)
                } else {
                    repository.getFamilyById(familyId).collect {
                        family.emit(it)
                    }

                    repository.getApplicationsToFamily(familyId).collect {
                        repository.getApplicants(it.map { application -> application.userId })
                            .collect { users ->
                                applicants.emit(users)
                            }
                    }
                }
            }
        }
    }

    fun updateFamilyName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateFamily(family.last()!!.id, newName)
            } catch (e: Exception) {
                errors.emit("Не удалось изменить название, попробуйте позднее")
            }
        }
    }

    fun getApplicants(): Flow<List<User>> = applicants

    fun getFamily(): Flow<Family?> = family

    fun approve(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.approveApplication(userId, family.last()!!.id)
            } catch (e: Exception) {
                errors.emit("Не удалось одобрить заявку, попробуйте позднее")
            }
        }
    }

    fun reject(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.rejectApplication(userId, family.last()!!.id)
            } catch (e: Exception) {
                errors.emit("Не удалось отменить заявку, попробуйте позднее")
            }
        }
    }
}