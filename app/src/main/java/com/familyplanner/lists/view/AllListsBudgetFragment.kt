package com.familyplanner.lists.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentSpendingsBinding
import com.familyplanner.lists.adapters.ListBudgetAdapter
import com.familyplanner.lists.model.BudgetDto
import com.familyplanner.lists.viewmodel.AllListsBudgetViewModel
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class AllListsBudgetFragment : Fragment() {
    private var _binding: FragmentSpendingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AllListsBudgetViewModel
    private val calendar = Calendar.getInstance()
    private val graphFormatter = XAxisDateFormatter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpendingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AllListsBudgetViewModel::class.java]
        val familyId = requireArguments().getString("familyId") ?: ""
        val budgetAdapter = ListBudgetAdapter()
        binding.rvBudget.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBudget.adapter = budgetAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getListsForFamily(familyId).collect {
                    requireActivity().runOnUiThread {
                        if (binding.rvBudget.isVisible) {
                            budgetAdapter.setData(it.sortedByDescending { it.addedAt })
                        } else {
                            updateGraph(it)
                        }
                        binding.tvSumValue.text = it.sumOf { it.sumSpent }.toString()
                        binding.tvDateStart.text =
                            viewModel.getStartDate().format(FamilyPlanner.uiDateFormatter)
                        binding.tvDateFinish.text =
                            viewModel.getFinishDate().format(FamilyPlanner.uiDateFormatter)
                    }
                }
            }
        }
        binding.ivAdd.isVisible = false
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvDateStart.setOnClickListener {
            setDate(it as TextView) {
                viewModel.updateStartDate(it)
            }
        }
        binding.tvDateFinish.setOnClickListener {
            setDate(it as TextView) {
                viewModel.updateFinishDate(it)
            }
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isList = binding.tabs.getTabAt(0)!!.equals(tab)
                viewModel.getLastSpendings()?.let {
                    if (isList) {
                        budgetAdapter.setData(it)
                    } else {
                        updateGraph(it)
                    }
                }
                binding.rvBudget.isVisible = isList
                binding.chart.isVisible = !isList
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

    private fun updateGraph(spendingHistory: List<BudgetDto>) {
        val spendingsByList = spendingHistory.groupBy { it.listId }
        val datasets = arrayListOf<ILineDataSet>()
        val startDate = viewModel.getStartDate().toEpochDay()
        val finishDate = viewModel.getFinishDate().toEpochDay()
        for (listId in spendingsByList.keys) {
            val completionDates =
                hashSetOf<Long>()
            completionDates.addAll(spendingsByList[listId]!!.map {
                it.addedAt.toLocalDate().toEpochDay()
            }.sorted())
            val userLine = arrayListOf<Entry>()
            (startDate..finishDate).forEach { date ->
                userLine.add(
                    Entry(
                        date.toFloat(),
                        spendingsByList[listId]!!.sumOf { it.sumSpent }.toFloat()
                    ),
                )
            }
            datasets.add(LineDataSet(userLine, spendingsByList[listId]!![0].listName))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}