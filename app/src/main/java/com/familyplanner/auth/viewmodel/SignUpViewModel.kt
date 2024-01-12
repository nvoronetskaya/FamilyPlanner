package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.auth.network.AuthQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {
    private val auth = AuthQueries()
    private val loggedIn = MutableSharedFlow<Boolean>()
    private val repo = UserRepository()

    fun isLoggedIn() = loggedIn

    fun finishSignUp(name: String, birthday: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = auth.signUp(email, password).await()
            if (result.user == null) {
                loggedIn.emit(false)
            } else {
                repo.addUser(name, birthday, email)

                val logIn = auth.signIn(email, password).await()
                if (logIn.user == null) {
                    loggedIn.emit(false)
                } else {
                    loggedIn.emit(true)
                }
            }
        }
    }
}