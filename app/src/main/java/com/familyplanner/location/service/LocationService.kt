package com.familyplanner.location.service

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.familyplanner.common.schema.UserDbSchema
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await


class LocationService : Service() {
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val firestore = Firebase.firestore
    private val userId = FamilyPlanner.userId
    private val notificationSender = LocationNotificationSender(this)
    private var notificationManager: NotificationManager? = null
    private var locationManager: LocationManager? = null
    private val LOCATION_ACTIVE = "Отслеживание местоположения включено"
    private val LOCATION_DISABLED =
        "Отслеживание местоположения приостановлено. Проверьте доступ к локации и подключение к сети"
    private val notification =
        NotificationCompat.Builder(this, "LOCATION").setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Отслеживание местоположения").setOngoing(true)
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                firestore.collection(UserDbSchema.USER_TABLE).document(userId)
                    .update(UserDbSchema.LOCATION, GeoPoint(it.latitude, it.longitude))
                notificationSender.onLocationUpdated(it.latitude, it.longitude)
            }
        }

        override fun onLocationAvailability(p0: LocationAvailability) {
            val contentText =
                if (p0.isLocationAvailable) LOCATION_ACTIVE else LOCATION_DISABLED
            notificationManager?.notify(1, notification.setContentText(contentText).build())
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationRequest = LocationRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS).build()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationSender.startUpdates()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            var message = LOCATION_ACTIVE
            try {
                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) {
                    message = LOCATION_DISABLED
                }
                if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) != true) {
                    message = LOCATION_DISABLED
                }
            } catch (ex: Exception) {
                message = LOCATION_DISABLED
            }
            notification.setContentText(message)
            startForeground(1, notification.build())
            mFusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}