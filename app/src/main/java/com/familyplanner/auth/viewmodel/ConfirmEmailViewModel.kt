package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.generateCode
import com.familyplanner.auth.sendSignUpCode
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.ktx.Firebase
import com.sun.mail.smtp.SMTPAddressFailedException
import kotlinx.coroutines.flow.MutableSharedFlow

class ConfirmEmailViewModel : ViewModel() {
    private val letterSent =  MutableSharedFlow<String>()
    private var firestore = Firebase.firestore
    private var code = ""

    fun getLetterSent() = letterSent

    fun sendConfirmationLetter(address: String) {
        firestore.collection("users").whereEqualTo("email", address).get().addOnCompleteListener {
                if (!it.isSuccessful) {
                    viewModelScope.launch {
                        letterSent.emit("Ошибка. Проверьте подключение к сети")
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
                            letterSent.emit("")
                        } catch (e: Exception) {
                            val errorMessage = if (e.cause is SMTPAddressFailedException) {
                                "Некорректный адрес почты" 
                            } else {
                                "Ошибка. Проверьте подключение к сети"
                            }
                            letterSent.emit(errorMessage)
                        }
                    }
                }
            }
    }

    fun getConfirmationCode() = code
}