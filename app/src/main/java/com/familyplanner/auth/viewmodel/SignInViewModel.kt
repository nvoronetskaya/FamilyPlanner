package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.auth.network.AuthQueries
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInViewModel : ViewModel() {
    private val auth = AuthQueries()
    private val userRepo = UserRepository()
    private val loggedIn = MutableSharedFlow<String>()

    fun isLoggedIn() = loggedIn

    fun resetPassword(email: String): Flow<String> {
        val errorMessage = MutableSharedFlow<String>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                auth.resetPassword(email).await()
                errorMessage.emit("")
            } catch (e: FirebaseNetworkException) {
                errorMessage.emit("Проверьте соединение и повторите позднее")
            } catch (e: Exception) {
                errorMessage.emit("Ошибка. Проверьте данные и попробуйте снова")
            }
        }
        return errorMessage
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logIn = auth.signIn(email, password).await()
                if (logIn.user == null) {
                    loggedIn.emit("Ошибка. Попробуйте позднее")
                } else {
                    userRepo.setFcmToken(logIn.user!!.uid, Firebase.messaging.token.await())
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