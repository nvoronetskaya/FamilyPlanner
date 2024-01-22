package com.familyplanner

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class FamilyPlanner: Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("YOUR_API_KEY")
    }
}