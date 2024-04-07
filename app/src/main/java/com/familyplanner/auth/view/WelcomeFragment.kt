package com.familyplanner.auth.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.familyplanner.R
import com.familyplanner.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment()  {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bStart.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_enterEmailFragment)
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_signInFragment)
        }
    }

    private fun startWorkManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notChan = NotificationChannel("1", "CHANNEL", NotificationManager.IMPORTANCE_HIGH)
            val nManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(notChan)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}