package com.familyplanner.auth

import java.util.*
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun generateCode(): String {
    val array = Array(6) { (0..9).random() }
    return array.joinToString("")
}

fun sendSignUpCode(email: String, message: String) {
    val props = Properties()
    props["mail.smtp.host"] = "smtp.gmail.com"
    props["mail.smtp.auth"] = "true"
    props["mail.smtp.ssl.enable"] = "true"
    props["mail.smtp.port"] = "465"
    props["mail.smtp.socketFactory.port"] = "465"
    props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"

    val session =
        Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication("noreply.familyplanner@gmail.com", "izridmtjecqcggcq")
            }
        })

    val mime = MimeMessage(session)
    mime.sender = InternetAddress("noreply.familyplanner@gmail.com")

    mime.setRecipients(Message.RecipientType.TO, email)
    mime.subject = "Регистрация в Семейном помощнике"
    mime.setText(message)

    Transport.send(mime)
}
