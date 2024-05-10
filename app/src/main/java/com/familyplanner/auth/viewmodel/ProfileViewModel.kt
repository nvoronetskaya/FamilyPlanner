package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.repository.UserRepository
import com.familyplanner.common.data.User
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private var user = MutableSharedFlow<User>(replay = 1)
    private val auth = Firebase.auth
    private val userRepo = UserRepository()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userRepo.getUserById(FamilyPlanner.userId).collect {
                user.emit(it)
            }
        }
    }

    fun getUser(): Flow<User> = user

    fun updateUserInfo(userId: String, name: String, birthday: String) {
        userRepo.updateUser(userId, name, birthday)
    }

    fun exit() {
        userRepo.removeFcmToken(FamilyPlanner.userId)
        auth.signOut()
    }

    fun changePassword(): Flow<String> {
        val errorMessage = MutableSharedFlow<String>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                auth.sendPasswordResetEmail(auth.currentUser!!.email!!).await()
                errorMessage.emit("")
            } catch (e: FirebaseNetworkException) {
                errorMessage.emit("Проверьте соединение и повторите позднее")
            } catch (e: Exception) {
                errorMessage.emit("Ошибка. Попробуйте снова позднее")
            }
        }
        return errorMessage
    }

    fun checkPassword(password: String): Task<Void>? {
        return userRepo.checkPassword(password)
    }
}