package com.familyplanner.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.common.repository.UserRepository
import com.familyplanner.auth.generateCode
import com.familyplanner.auth.sendSignUpCode
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sun.mail.smtp.SMTPAddressFailedException
import kotlinx.coroutines.flow.MutableSharedFlow

class ConfirmEmailViewModel : ViewModel() {
    private val letterSent = MutableSharedFlow<String>()
    private var code = ""
    private var userRepo = UserRepository()

    fun getLetterSent() = letterSent

    fun sendConfirmationLetter(address: String, isChangeEmail: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val message: String
            try {
                if (!userRepo.hasAccount(address)) {
                    code = generateCode()
                    message = if (!isChangeEmail)
                        "Здравствуйте! \nДля продолжения регистрации в \"Семейном планировщике\" введите следующий код подтверждения: $code"
                    else "Здравствуйте! \n" +
                            "Для смены почты в \"Семейном планировщике\" введите следующий код подтверждения: $code"
                } else {
                    message =
                        "Вы уже зарегистрированы в приложении \"Семейный планировщик\". В случае утери пароля воспользуйтесь функцией его восстановления"
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                letterSent
                    .emit("Некорректный адрес почты")
                return@launch
            } catch (e: FirebaseNetworkException) {
                letterSent
                    .emit("Ошибка. Проверьте подключение к сети")
                return@launch
            }
            val subject = if (!isChangeEmail) "Регистрация в Семейном планировщике" else "Смена почты в Семейном планировщике"
            try {
                sendSignUpCode(address, message, subject)
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