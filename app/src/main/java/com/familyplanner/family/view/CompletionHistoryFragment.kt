package com.familyplanner.family.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentStatsBinding
import com.familyplanner.family.data.CompletionAdapter
import com.familyplanner.family.data.CompletionDto
import com.familyplanner.family.viewmodel.CompletionHistoryViewModel
import com.familyplanner.lists.view.XAxisDateFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class CompletionHistoryFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CompletionHistoryViewModel
    private val calendar: Calendar = Calendar.getInstance()
    private val graphFormatter = XAxisDateFormatter()

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
        val familyId = requireArguments().getString("familyId")
        if (familyId == null) {
            findNavController().popBackStack()
            return
        }
        binding.tvDateStart.text =
            LocalDate.ofEpochDay(viewModel.getStartDay()).format(FamilyPlanner.uiDateFormatter)
        binding.tvDateFinish.text =
            LocalDate.ofEpochDay(viewModel.getFinishDay()).format(FamilyPlanner.uiDateFormatter)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CompletionAdapter(::openTask)
        binding.rvHistory.adapter = adapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setFamily(familyId).collect {
                    requireActivity().runOnUiThread {
                        if (_binding == null) {
                            return@runOnUiThread
                        }
                        if (binding.rvHistory.isVisible) {
                            adapter.setData(it)
                        } else {
                            updateGraph(it)
                        }
                    }
                }
            }
        }
        binding.tvDateStart.setOnClickListener {
            setDate(it as TextView) { date ->
                viewModel.updateStart(date.toEpochDay())
            }
        }

        binding.tvDateFinish.setOnClickListener {
            setDate(it as TextView) { date ->
                viewModel.updateFinish(date.toEpochDay())
            }
        }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val lastValues = viewModel.lastCompletionValues()
                if (binding.tabs.getTabAt(0)!!.equals(tab)) {
                    lastValues?.let {
                        adapter.setData(it)
                    }
                    binding.rvHistory.isVisible = true
                    binding.chart.isVisible = false
                } else {
                    lastValues?.let {
                        updateGraph(it)
                    }
                    binding.rvHistory.isVisible = false
                    binding.chart.isVisible = true
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun setDate(dateHolder: TextView, onDateChosen: (LocalDate) -> Unit) {
        val dialog = DatePickerDialog(
            activity as MainActivity,
            R.style.datePickerDialog,
            { _, year, month, day ->
                val date = LocalDate.of(year, month + 1, day)
                dateHolder.text =
                    date.format(FamilyPlanner.uiDateFormatter)
                onDateChosen(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = calendar.timeInMillis
        dialog.show()
    }

    private fun updateGraph(completionHistory: List<CompletionDto>) {
        val completionByUser = completionHistory.groupBy { it.userId }
        val datasets = arrayListOf<ILineDataSet>()
        val startDate = viewModel.getStartDay()
        val finishDate = viewModel.getFinishDay()
        for (userId in completionByUser.keys) {
            val completionDates =
                hashSetOf<Long>()
            completionDates.addAll(completionByUser[userId]!!.map { it.completionDate }.sorted())
            val userLine = arrayListOf<Entry>()
            (startDate..finishDate).forEach { date ->
                userLine.add(
                    Entry(
                        date.toFloat(),
                        completionByUser[userId]!!.count { it.completionDate == date }.toFloat()
                    ),
                )
            }
            datasets.add(LineDataSet(userLine, completionByUser[userId]!!.get(0).userName))
        }
        val chart = binding.chart
        chart.data = LineData(datasets)
        val xAxis = chart.xAxis
        xAxis.labelCount = (finishDate - startDate + 1).toInt()
        xAxis.valueFormatter = graphFormatter
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.labelRotationAngle = 315f
        chart.invalidate()
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