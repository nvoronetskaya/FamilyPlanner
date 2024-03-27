package com.familyplanner

import android.app.Application

class FamilyPlanner: Application() {
    companion object {
        var isInit = false
    }
    override fun onCreate() {
        super.onCreate()
    }
}