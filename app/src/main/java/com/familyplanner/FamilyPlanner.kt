package com.familyplanner

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

class FamilyPlanner: Application() {
    companion object {
        var isInit = false
        var userId = Firebase.auth.currentUser?.uid ?: ""
        val uiDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
    override fun onCreate() {
        super.onCreate()
    }
}