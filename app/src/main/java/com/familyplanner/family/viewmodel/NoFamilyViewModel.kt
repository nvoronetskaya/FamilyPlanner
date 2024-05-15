package com.familyplanner.family.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.repository.UserRepository
import com.familyplanner.common.data.User
import com.familyplanner.family.repository.FamilyRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoFamilyViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val user = MutableSharedFlow<User>(replay = 1)
    private val userRepo = UserRepository()
    private val familyRepo = FamilyRepository()
    private val errors = MutableSharedFlow<String>()

    init {
        FamilyPlanner.updateUserId()
        viewModelScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                userRepo.setFcmToken(FamilyPlanner.userId, com.google.firebase.Firebase.messaging.token.await())
            }
            userRepo.getUserById(FamilyPlanner.userId).collect {
                it?.let { user.emit(it) }
            }
        }
    }

    fun getErrors() = errors

    fun getUser(): Flow<User> = user

    fun createFamily(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                errors.emit("Не удалось создать семью, попробуйте позднее")
                return@launch
            }
            familyRepo.createFamily(name, userId)
        }
    }

    fun joinFamily(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            familyRepo.applyToFamily(FamilyPlanner.userId, code)
        }
    }
}