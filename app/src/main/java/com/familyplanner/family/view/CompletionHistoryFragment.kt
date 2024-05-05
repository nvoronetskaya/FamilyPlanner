package com.familyplanner.family.view

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.familyplanner.databinding.FragmentStatsBinding
import com.familyplanner.family.data.CompletionAdapter
import com.familyplanner.family.viewmodel.CompletionHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class CompletionHistoryFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CompletionHistoryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CompletionHistoryViewModel::class.java]
        binding.tvDateStart.text =
            LocalDate.ofEpochDay(viewModel.getStartDay()).format(FamilyPlanner.uiDateFormatter)
        binding.tvDateFinish.text =
            LocalDate.ofEpochDay(viewModel.getFinishDay()).format(FamilyPlanner.uiDateFormatter)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CompletionAdapter(::openTask)
        binding.rvHistory.adapter = adapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getHistory().collect {
                    adapter.setData(it)
                }
            }
        }
    }

    private fun openTask(taskId: String) {
        findNavController().navigate(
            R.id.action_completionHistoryFragment_to_showTaskInfoFragment,
            bundleOf("taskId" to taskId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}