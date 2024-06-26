package com.familyplanner.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.common.repository.UserRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActivityViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val isLogged = MutableStateFlow(auth.currentUser != null)
    private val hasFamilyFlow = MutableSharedFlow<Boolean>(replay = 1)
    private var hasFamily: Boolean = false
    private val userRepository = UserRepository()
    private var userId: String? = null
    private var familyFlowJob: Job? = null

    fun isLoggedIn(): Flow<Boolean> = isLogged

    init {
        auth.addAuthStateListener {
            viewModelScope.launch(Dispatchers.IO) {
                startUserUpdates(it.currentUser?.uid)
                if (it.currentUser == null && isLogged.value) {
                    isLogged.emit(false)
                } else if (it.currentUser != null && !isLogged.value) {
                    isLogged.emit(true)
                }
            }
        }
    }

    private fun startUserUpdates(userId: String?) {
        if (this.userId == userId) {
            return
        }
        this.userId = userId
        viewModelScope.launch {
            familyFlowJob?.cancelAndJoin()
            if (userId.isNullOrEmpty()) {
                return@launch
            }
            familyFlowJob = launch(Dispatchers.IO) {
                userRepository.getUserById(userId).collect {
                    hasFamily = !(it == null || it.familyId.isNullOrEmpty())
                    hasFamilyFlow.emit(hasFamily)
                }
            }
            familyFlowJob?.start()
        }
    }

    fun getHasFamilyUpdates(): Flow<Boolean> = hasFamilyFlow

    fun getHasFamily(): Boolean = hasFamily

    fun updateFcmToken() {
        viewModelScope.launch(Dispatchers.IO) {
            auth.currentUser?.uid?.let {
                userRepository.setFcmToken(it, Firebase.messaging.token.await())
            }
        }
    }
}