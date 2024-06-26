package com.familyplanner.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.familyplanner.common.view.MainActivity
import com.familyplanner.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FamilyPlanner.userId.isNotEmpty()) {
            findNavController().navigate(
                R.id.noFamilyFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
            )
        }
        (requireActivity() as MainActivity).hideBottomNavigation()
        binding.bStart.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_enterEmailFragment)
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_signInFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}