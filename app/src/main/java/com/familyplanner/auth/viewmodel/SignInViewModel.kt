package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.auth.network.AuthQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInViewModel : ViewModel() {
    private val auth = AuthQueries()
    private val userRepo = UserRepository()
    private val loggedIn = MutableSharedFlow<Boolean>()

    fun isLoggedIn() = loggedIn

    fun resetPassword(email: String) {
        auth.resetPassword(email)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val logIn = auth.signIn(email, password).await()
            if (logIn.user == null) {
                loggedIn.emit(false)
            } else {
                loggedIn.emit(true)
            }
        }
    }
}