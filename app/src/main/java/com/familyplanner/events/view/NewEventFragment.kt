package com.familyplanner.events.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.familyplanner.databinding.FragmentNewEventBinding
import com.familyplanner.events.viewmodel.NewEventViewModel

class NewEventFragment : Fragment() {
    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NewEventViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NewEventViewModel::class.java]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}