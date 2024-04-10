package com.familyplanner.events.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.FragmentEventInfoBinding
import com.familyplanner.events.adapters.AttendeeAdapter
import com.familyplanner.events.viewmodel.EventInfoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
        val eventId = requireArguments().getString("eventId")!!
        viewModel.setEvent(eventId)
        val attendeeAdapter = AttendeeAdapter()
        val attendeeLayoutManager = LinearLayoutManager(context)
        binding.rvObservers.layoutManager = attendeeLayoutManager
        binding.rvObservers.adapter = attendeeAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getEvent().collect {
                        requireActivity().runOnUiThread {
                            if (it == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Мероприятие не найдено",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                binding.etName.setText(it.name)
                                binding.tfDescription.isVisible = it.description.isNotBlank()
                                binding.etDescription.setText(it.description)
                                binding.tvStartValue.text = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(it.start),
                                    ZoneId.systemDefault()
                                ).format(FamilyPlanner.dateTimeFormatter)
                                binding.tvFinishValue.text = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(it.start),
                                    ZoneId.systemDefault()
                                ).format(FamilyPlanner.dateTimeFormatter)
                                binding.ivEdit.isVisible = FamilyPlanner.userId.equals(it.createdBy)
                                binding.tvCancel.isVisible = FamilyPlanner.userId.equals(it.createdBy)
                                binding.ivEdit.setOnClickListener {
                                    TODO()
                                }
                                binding.tvCancel.setOnClickListener {
                                    viewModel.deleteEvent(eventId)
                                    findNavController().popBackStack()
                                }
                                binding.cbParticipate.setOnClickListener {
                                    viewModel.changeComing(FamilyPlanner.userId, eventId, binding.cbParticipate.isChecked)
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.getAttendees().collect {
                        requireActivity().runOnUiThread {
                            attendeeAdapter.setData(it)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}