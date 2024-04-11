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

class MembersListViewModel : ViewModel() {
    private val userId: String = FamilyPlanner.userId
    private val errors = MutableSharedFlow<String>()
    private val repository = FamilyRepository()
    private var family = MutableSharedFlow<Family?>()
    private var members = MutableSharedFlow<List<User>>()

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

                    repository.getFamilyMembers(familyId).collect {
                        members.emit(it)
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
}