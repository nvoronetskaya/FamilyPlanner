package com.familyplanner.tasks.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
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
    ): View {
        _binding = FragmentTaskObserversBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val familyId = requireArguments().getString("familyId")!!
        val taskId = requireArguments().getString("taskId")!!
        viewModel = ViewModelProvider(this)[TaskObserversViewModel::class.java]

        val adapter = AddTaskObserverAdapter(FamilyPlanner.userId)
        binding.rvObservers.layoutManager = LinearLayoutManager(activity)
        binding.rvObservers.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            val members = viewModel.getObservers(familyId)
            requireActivity().runOnUiThread {
                if (_binding == null) {
                    return@runOnUiThread
                }
                adapter.setData(members)
            }
        }

        binding.ivClose.setOnClickListener {
            findNavController().navigate(
                R.id.action_newTaskObserversFragment_to_tasksListFragment,
                bundleOf("familyId" to familyId)
            )
        }

        binding.ivDone.setOnClickListener {
            viewModel.setObserversAndExecutors(taskId)
            findNavController().navigate(
                R.id.action_newTaskObserversFragment_to_tasksListFragment,
                bundleOf("familyId" to familyId)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}