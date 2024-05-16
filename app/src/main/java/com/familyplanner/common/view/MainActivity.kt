package com.familyplanner.common.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.familyplanner.common.viewmodel.ActivityViewModel
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.familyplanner.databinding.ActivityMainBinding
import com.familyplanner.location.service.LocationService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ActivityViewModel
    private lateinit var navController: NavController
    private val allowedNoFamilyDestinations = setOf(
        R.id.noFamilyFragment,
        R.id.profileFragment,
        R.id.enterEmailFragment,
        R.id.confirmEmailFragment,
        R.id.enterInfoFragment
    )

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
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        val isWelcomeFragment = getCurrentDestinationId() == R.id.welcomeFragment
        binding.bottomNavigation.isVisible = !isWelcomeFragment
        if (Firebase.auth.currentUser != null) {
            viewModel.updateFcmToken()
            if (isWelcomeFragment) {
                navController.navigate(R.id.action_welcomeFragment_to_noFamilyFragment)
                binding.bottomNavigation.visibility = View.VISIBLE
            }
        }
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
        val locationService = Intent(applicationContext, LocationService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getHasFamilyUpdates().collect {
                    runOnUiThread {
                        if (Firebase.auth.currentUser == null) {
                            stopService(locationService)
                            navController.navigate(
                                R.id.welcomeFragment,
                                null,
                                NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                            )
                            return@runOnUiThread
                        }
                        if (it) {
                            startService(locationService)
                        } else {
                            stopService(locationService)
                        }
                        val currentDestination = getCurrentDestinationId()
                        if (!it && !allowedNoFamilyDestinations.contains(currentDestination)) {
                            navController.navigate(
                                R.id.noFamilyFragment,
                                null,
                                NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                            )
                        }
                    }
                }
            }
        }
        binding.fragmentContainerView.visibility = View.VISIBLE
        setUpBottomNavigation()
    }

    fun showBottomNavigation() {
        binding.bottomNavigation.isVisible = true
        setUpBottomNavigation()
    }

    private fun setUpBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener {
            if (!viewModel.getHasFamily()) {
                if (it.itemId == R.id.home) {
                    navController.navigate(
                        R.id.noFamilyFragment,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                    )
                    return@setOnItemSelectedListener true
                }
                return@setOnItemSelectedListener false
            }
            when (it.itemId) {
                R.id.home -> navController.navigate(
                    R.id.noFamilyFragment,
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
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetworkInfo
        return network != null && network.isConnected
    }

    private fun getCurrentDestinationId(): Int? = navController.currentDestination?.id
}