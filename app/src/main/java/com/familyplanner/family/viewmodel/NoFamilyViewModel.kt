package com.familyplanner.family.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.common.User
import com.familyplanner.family.data.FamilyRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class NoFamilyViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val user = MutableSharedFlow<User>(replay = 1)
    private val userRepo = UserRepository()
    private val familyRepo = FamilyRepository()
    private val errors = MutableSharedFlow<String>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userRepo.getUserByEmail(auth.currentUser!!.email!!).collect {
                user.emit(it)
            }
        }
    }

    fun getErrors() = errors

    fun getUser(): Flow<User> = user

    fun createFamily(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userDoc = user.replayCache[0]
            val userId = userDoc.id
            familyRepo.createFamily(name, userId).addOnCompleteListener {
                if (it.isSuccessful) {
                    familyRepo.setUserToAdmin(userId, it.result.id)
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        errors.emit("Не удалось создать семью, попробуйте позднее")
                    }
                }
            }
        }
    }

    fun joinFamily(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            familyRepo.applyToFamily(user.last().id, code)
        }
    }
}