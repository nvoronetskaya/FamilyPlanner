package com.familyplanner

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.familyplanner.auth.sendSignUpCode
import com.familyplanner.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        // binding.bottomNavigation.visibility = View.GONE
    }
}