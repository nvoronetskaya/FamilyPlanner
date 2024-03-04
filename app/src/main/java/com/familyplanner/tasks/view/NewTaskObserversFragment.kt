package com.familyplanner.tasks.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.R
import com.familyplanner.databinding.FragmentTaskObserversBinding
import com.familyplanner.tasks.adapters.AddTaskObserverAdapter
import com.familyplanner.tasks.viewmodel.TaskObserversViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewTaskObserversFragment : Fragment() {
    private var _binding: FragmentTaskObserversBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TaskObserversViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskObserversBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val familyId = requireArguments().getString("familyId")!!
        val taskId = requireArguments().getString("taskId")!!
        viewModel = ViewModelProvider(this)[TaskObserversViewModel::class.java]
        viewModel.setFamily(familyId)

        val adapter = AddTaskObserverAdapter()
        binding.rvObservers.layoutManager = LinearLayoutManager(activity)
        binding.rvObservers.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getMembers().collect {
                    adapter.setData(it)
                }
            }
        }

        binding.ivClose.setOnClickListener {
            findNavController().navigate(R.id.action_newTaskObserversFragment_to_tasksListFragment)
        }

        binding.ivDone.setOnClickListener {
            viewModel.setObserversAndExecutors(
                adapter.getMembers(),
                adapter.getObservers(),
                adapter.getExecutors(),
                taskId
            )
            findNavController().navigate(R.id.action_newTaskObserversFragment_to_tasksListFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}