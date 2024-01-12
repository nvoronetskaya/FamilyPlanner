package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.generateCode
import com.familyplanner.auth.sendSignUpCode
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow

class ConfirmEmailViewModel : ViewModel() {
    private val letterSent =  MutableSharedFlow<Boolean>()
    private var firestore = Firebase.firestore
    private var code = ""

    fun getLetterSent() = letterSent

    fun sendConfirmationLetter(address: String) {
        firestore.collection("users").whereEqualTo("email", address).get().addOnCompleteListener {
                if (!it.isSuccessful) {
                    viewModelScope.launch {
                        letterSent.emit(false)
                    }
                } else {
                    var message = ""
                    if (it.result.isEmpty) {
                        code = generateCode()
                        message = code
                    } else {
                        message = "Вы уже зарегистрированы"
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            sendSignUpCode(address, message)
                            letterSent.emit(true)
                        } catch (e: Exception) {
                            letterSent.emit(false)
                        }
                    }
                }
            }
    }

    fun getConfirmationCode() = code
}