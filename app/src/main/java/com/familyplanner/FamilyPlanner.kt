package com.familyplanner

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat

class FamilyPlanner: Application() {
    companion object {
        var isInit = false
        var userId = Firebase.auth.currentUser?.uid ?: ""
        val uiDateFormatter = SimpleDateFormat("dd.MM.yyyy")
    }
    override fun onCreate() {
        super.onCreate()
    }
}