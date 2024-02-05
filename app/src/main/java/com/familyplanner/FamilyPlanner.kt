package com.familyplanner

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class FamilyPlanner: Application() {
    override fun onCreate() {
        MapKitFactory.setApiKey("20c53eda-cff4-4d4e-bbac-f2d4a5cda330")

        super.onCreate()
    }
}