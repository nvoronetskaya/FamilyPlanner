package com.familyplanner

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.format.DateTimeFormatter

class FamilyPlanner : Application() {
    companion object {
        var isInit = false
        var userId = Firebase.auth.currentUser?.uid ?: ""
        val uiDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        fun updateUserId() {
            userId = Firebase.auth.currentUser?.uid ?: ""
        }
    }
}