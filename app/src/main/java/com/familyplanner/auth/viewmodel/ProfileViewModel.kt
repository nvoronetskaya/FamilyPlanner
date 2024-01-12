package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.common.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private var user = MutableSharedFlow<User>(replay = 1)
    private val auth = Firebase.auth
    private val userRepo = UserRepository()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userRepo.getUserByEmail(auth.currentUser!!.email!!).collect {
                user.emit(it[0])
            }
        }
    }

    fun getUser(): Flow<User> = user

    fun updateUserInfo(userId: String, name: String, birthday: String) {
        userRepo.updateUser(userId, name, birthday)
    }

    fun exit() {
        auth.signOut()
    }

    fun changePassword() {
        auth.sendPasswordResetEmail(auth.currentUser!!.email!!)
    }

    fun changeEmail(newEmail: String, password: String): Flow<Boolean> {
        val currentUser = auth.currentUser
        val credentials = EmailAuthProvider.getCredential(
            currentUser!!.email!!,
            password
        )

        val wasSuccessful = MutableSharedFlow<Boolean>(1)
        currentUser.reauthenticate(credentials).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                currentUser.verifyBeforeUpdateEmail(newEmail)
                    .addOnCompleteListener { update ->
                        viewModelScope.launch(Dispatchers.IO) {
                            wasSuccessful.emit(update.isSuccessful)
                        }
                    }
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    wasSuccessful.emit(false)
                }
            }
        }

        return wasSuccessful
    }
}