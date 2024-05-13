package com.familyplanner.lists.view

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentSpendingsBinding
import com.familyplanner.lists.adapters.ListBudgetAdapter
import com.familyplanner.lists.viewmodel.ListBudgetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class ListBudgetFragment : Fragment() {
    private var _binding: FragmentSpendingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ListBudgetViewModel
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
        viewModel = ViewModelProvider(this)[ListBudgetViewModel::class.java]
        val listId = requireArguments().getString("listId") ?: ""
        val budgetAdapter = ListBudgetAdapter()
        binding.rvBudget.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBudget.adapter = budgetAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            val spendings = viewModel.getSpendings(listId)
            requireActivity().runOnUiThread {
                if (_binding == null) {
                    return@runOnUiThread
                }
                budgetAdapter.setData(spendings.sortedByDescending { it.addedAt })
                binding.tvSumValue.text = spendings.sumOf { it.sumSpent }.toString()
                binding.tvDateStart.text =
                    viewModel.getStartDate().format(FamilyPlanner.uiDateFormatter)
                binding.tvDateFinish.text =
                    viewModel.getFinishDate().format(FamilyPlanner.uiDateFormatter)
            }
        }
        binding.tabs.isVisible = false
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.ivAdd.setOnClickListener {
            val moneySpent = EditText(activity)
            moneySpent.typeface =
                Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
            moneySpent.hint = "Введите сумму"
            moneySpent.textSize = 17F
            moneySpent.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Добавление траты").setView(moneySpent, 40, 0, 40, 0)
                .setPositiveButton("Готово") { _, _ ->
                    if (moneySpent.text.isNullOrBlank()) {
                        moneySpent.error = "Введите сумму"
                    } else {
                        try {
                            val moneySpentDouble = moneySpent.text.trim().toString().toDouble()
                            if (moneySpentDouble < 1 || moneySpentDouble > 1_000_000) {
                                moneySpent.error = "Введите значение от 1 до 1 000 000"
                            } else {
                                viewModel.addSpending(moneySpent.text.trim().toString().toDouble(), listId)
                            }
                        } catch (e: Exception) {
                            moneySpent.error = "Некорректное значение"
                        }
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }
        binding.tvDateStart.setOnClickListener {
            setDate(it as TextView) {
                val spendings = viewModel.updateStartDate(it)
                budgetAdapter.setData(spendings)
                binding.tvSumValue.text = spendings.sumOf { it.sumSpent }.toString()
            }
        }
        binding.tvDateFinish.setOnClickListener {
            setDate(it as TextView) {
                val spendings = viewModel.updateFinishDate(it)
                budgetAdapter.setData(spendings)
                binding.tvSumValue.text = spendings.sumOf { it.sumSpent }.toString()
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}