package com.familyplanner

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class FamilyPlanner: Application() {
    companion object {
        var isInit = false
    }
    override fun onCreate() {
        super.onCreate()
    }
}