package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.repository.UserRepository
import com.familyplanner.auth.network.AuthQueries
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {
    private val auth = AuthQueries()
    private val loggedIn = MutableSharedFlow<String>()
    private val repo = UserRepository()

    fun isLoggedIn() = loggedIn

    fun finishSignUp(name: String, birthday: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = auth.signUp(email, password).await()
                if (result.user == null) {
                    loggedIn.emit("Ошибка. Попробуйте позднее")
                } else {
                    repo.addUser(name, birthday, email, result.user!!.uid)
                    val logIn = auth.signIn(email, password).await()
                    if (logIn.user == null) {
                        loggedIn.emit("Ошибка. Попробуйте позднее")
                    } else {
                        loggedIn.emit("")
                        FamilyPlanner.userId = logIn.user!!.uid
                    }
                }
            } catch (e: FirebaseNetworkException) {
                loggedIn.emit("Проверьте подключение к сети и попробуйте позднее")
            } catch (e: Exception) {
                loggedIn.emit("Ошибка. Попробуйте позднее")
            }
        }
    }
}