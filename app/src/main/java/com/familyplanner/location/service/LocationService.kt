package com.familyplanner.location.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.familyplanner.common.schema.UserDbSchema
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore


class LocationService : Service() {
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val firestore = Firebase.firestore
    private val userId = FamilyPlanner.userId
    private val notificationSender = LocationNotificationSender(this)
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                firestore.collection(UserDbSchema.USER_TABLE).document(userId)
                    .update(UserDbSchema.LOCATION, GeoPoint(it.latitude, it.longitude))
                notificationSender.onLocationUpdated(it.latitude, it.longitude)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationRequest = LocationRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS).build()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification =
                NotificationCompat.Builder(this, "LOCATION").setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Отслеживание местоположения")
                    .setContentText("Отслеживание местоположения включено").setOngoing(true).build()
            mFusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
            startForeground(1, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}