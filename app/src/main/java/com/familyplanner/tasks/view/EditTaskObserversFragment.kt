package com.familyplanner.tasks.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.FragmentTaskObserversBinding
import com.familyplanner.tasks.adapters.AddTaskObserverAdapter
import com.familyplanner.tasks.viewmodel.EditTaskObserversViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditTaskObserversFragment : Fragment() {
    private var _binding: FragmentTaskObserversBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EditTaskObserversViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskObserversBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val taskId = requireArguments().getString("taskId")!!
        viewModel = ViewModelProvider(this)[EditTaskObserversViewModel::class.java]
        val observersAdapter = AddTaskObserverAdapter(FamilyPlanner.userId)
        binding.rvObservers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObservers.adapter = observersAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            val observers = viewModel.getObservers(taskId)
            requireActivity().runOnUiThread {
                if (_binding == null) {
                    return@runOnUiThread
                }
                observersAdapter.setData(observers)
            }
        }
        binding.ivDone.setOnClickListener {
            viewModel.updateObservers(taskId)
            findNavController().popBackStack()
        }
        binding.ivClose.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}