package com.familyplanner.events.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.familyplanner.databinding.FragmentEventInfoBinding
import com.familyplanner.events.viewmodel.EventInfoViewModel

class EventInfoFragment : Fragment() {
    private var _binding: FragmentEventInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EventInfoViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[EventInfoViewModel::class.java]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}