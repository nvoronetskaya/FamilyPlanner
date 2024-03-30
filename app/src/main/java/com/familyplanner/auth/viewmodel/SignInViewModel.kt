package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.auth.network.AuthQueries
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInViewModel : ViewModel() {
    private val auth = AuthQueries()
    private val userRepo = UserRepository()
    private val loggedIn = MutableSharedFlow<String>()

    fun isLoggedIn() = loggedIn

    fun resetPassword(email: String) {
        auth.resetPassword(email)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logIn = auth.signIn(email, password).await()
                if (logIn.user == null) {
                    loggedIn.emit("Ошибка. Попробуйте позднее")
                } else {
                    loggedIn.emit("")
                    FamilyPlanner.userId = logIn.user!!.uid
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                loggedIn.emit("Ошибка. Проверьте данные и попробуйте снова")
            } catch (e: FirebaseNetworkException) {
                loggedIn.emit("Проверьте подключение к сети и попробуйте позднее")
            } catch (e: Exception) {
                loggedIn.emit("Ошибка. Попробуйте позднее")
            }
        }
    }
}