package com.familyplanner

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
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
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
        viewModel = ViewModelProvider(this)[ActivityViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.bottomNavigation.visibility = View.GONE

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController
/*        if (Firebase.auth.currentUser != null) {
            navController.navigate(R.id.action_welcomeFragment_to_noFamilyFragment)
            binding.bottomNavigation.visibility = View.VISIBLE
        }*/
        binding.fragmentContainerView.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}