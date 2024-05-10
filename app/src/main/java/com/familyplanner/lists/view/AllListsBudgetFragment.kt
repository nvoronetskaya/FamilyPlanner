package com.familyplanner.lists.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
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
            val spendings = viewModel.getSpendings(familyId)
            requireActivity().runOnUiThread {
                if (binding.rvBudget.isVisible) {
                    budgetAdapter.setData(spendings.sortedByDescending { it.addedAt })
                } else {
                    updateGraph(spendings)
                }
                binding.tvSumValue.text = spendings.sumOf { it.sumSpent }.toString()
                binding.tvDateStart.text =
                    viewModel.getStartDate().format(FamilyPlanner.uiDateFormatter)
                binding.tvDateFinish.text =
                    viewModel.getFinishDate().format(FamilyPlanner.uiDateFormatter)
            }
        }
        binding.ivAdd.isVisible = false
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvDateStart.setOnClickListener {
            setDate(it as TextView) {
                val spendings = viewModel.updateStartDate(it)
                budgetAdapter.setData(spendings.sortedByDescending { it.addedAt })
                binding.tvSumValue.text = spendings.sumOf { it.sumSpent }.toString()
            }
        }
        binding.tvDateFinish.setOnClickListener {
            setDate(it as TextView) {
                val spendings = viewModel.updateFinishDate(it)
                budgetAdapter.setData(spendings.sortedByDescending { it.addedAt })
                binding.tvSumValue.text = spendings.sumOf { it.sumSpent }.toString()
            }
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isList = binding.tabs.getTabAt(0)!!.equals(tab)
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

    private fun updateGraph(completionHistory: List<BudgetDto>) {
        val spendingsByList = completionHistory.groupBy { it.listId }
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
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float, axis: AxisBase): String {
                return LocalDate.ofEpochDay(value.toLong()).format(FamilyPlanner.uiDateFormatter)
            }
        }
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}