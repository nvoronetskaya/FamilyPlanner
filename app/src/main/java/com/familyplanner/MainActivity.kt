package com.familyplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.familyplanner.auth.view.WelcomeFragment
import com.familyplanner.databinding.ActivityMainBinding
import com.familyplanner.family.view.NoFamilyFragment
import com.familyplanner.location.data.LocationService
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ActivityViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!FamilyPlanner.isInit) {
            FamilyPlanner.isInit = true
            MapKitFactory.setApiKey("20c53eda-cff4-4d4e-bbac-f2d4a5cda330")
        }
        MapKitFactory.initialize(this)

        supportActionBar?.hide()
        viewModel = ViewModelProvider(this)[ActivityViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.bottomNavigation.visibility = View.GONE

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        if (Firebase.auth.currentUser != null) {
            navController.navigate(R.id.action_welcomeFragment_to_noFamilyFragment)
            binding.bottomNavigation.visibility = View.VISIBLE
        }
        binding.fragmentContainerView.visibility = View.VISIBLE
        setUpBottomNavigation()
        val notChan =
            NotificationChannel("DATA_UPDATES", "New data", NotificationManager.IMPORTANCE_DEFAULT)
        val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.createNotificationChannel(notChan)
        val locationChan =
            NotificationChannel(
                "LOCATION",
                "Location updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        nManager.createNotificationChannel(locationChan)
        startService(Intent(applicationContext, LocationService::class.java))
    }

    fun showBottomNavigation() {
        binding.bottomNavigation.isVisible = true
        setUpBottomNavigation()
    }

    private fun setUpBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> navController.navigate(
                    R.id.tasksListFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                )

                R.id.map -> {
                    navController.navigate(
                        R.id.familyLocationFragment,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                    )
                }

                R.id.calendar -> {
                    navController.navigate(
                        R.id.eventsListFragment,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                    )
                }

                R.id.lists -> navController.navigate(
                    R.id.listsListFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                )
            }
            true
        }
    }

    fun hideBottomNavigation() {
        binding.bottomNavigation.isVisible = false
    }

    fun setToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    fun isConnectedToInternet(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetworkInfo
        return network != null && network.isConnected
    }
}