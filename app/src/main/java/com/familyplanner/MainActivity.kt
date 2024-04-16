package com.familyplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.SearchManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
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
        val notChan = NotificationChannel("10", "CHANNEL", NotificationManager.IMPORTANCE_HIGH)
        val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.createNotificationChannel(notChan)
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
}