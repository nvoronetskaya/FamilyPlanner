package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.auth.generateCode
import com.familyplanner.auth.sendSignUpCode
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.ktx.Firebase
import com.sun.mail.smtp.SMTPAddressFailedException
import kotlinx.coroutines.flow.MutableSharedFlow

class ConfirmEmailViewModel : ViewModel() {
    private val letterSent = MutableSharedFlow<String>()
    private var code = ""
    private var userRepo = UserRepository()

    fun getLetterSent() = letterSent

    fun sendConfirmationLetter(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val message: String
            if (!userRepo.hasAccount(address)) {
                code = generateCode()
                message =
                    "Здравствуйте! \nДля продолжения регистрации в \"Семейном планировщике\" введите следующий код подтверждения: $code"
            } else {
                message =
                    "Вы уже зарегистрированы в приложении \"Семейный планировщик\". В случае утери пароля воспользуйтесь функцией его восстановления"
            }
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

    fun getConfirmationCode() = code

    suspend fun changeEmail(password: String, newEmail: String): Task<Void>? {
        return userRepo.changeEmail(password, newEmail)
    }
}