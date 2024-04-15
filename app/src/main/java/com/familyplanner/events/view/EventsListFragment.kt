package com.familyplanner.events.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.familyplanner.R
import com.familyplanner.databinding.FragmentEventsListBinding
import com.familyplanner.events.viewmodel.EventsListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class EventsListFragment : Fragment() {
    private var _binding: FragmentEventsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EventsListViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[EventsListViewModel::class.java]
        binding.tvCurMonth.text = viewModel.currentMonthString()
        binding.ivPrevMonth.setOnClickListener {
            binding.tvCurMonth.text = viewModel.previousMonth()
        }
        binding.ivNextMonth.setOnClickListener {
            binding.tvCurMonth.text = viewModel.nextMonth()
        }
        binding.ivAdd.setOnClickListener {
            findNavController().navigate(R.id.action_eventsListFragment_to_newEventFragment)
        }
        binding.calendar.setOnEventChosen {
            findNavController().navigate(
                R.id.action_eventsListFragment_to_eventInfoFragment,
                bundleOf("eventId" to it)
            )
        }
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getEvents().collect {
                    binding.calendar.updateEvents(it, viewModel.currentMonth())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}