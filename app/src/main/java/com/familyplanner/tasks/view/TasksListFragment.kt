package com.familyplanner.tasks.view

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
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
import com.familyplanner.databinding.BottomsheetTaskFiltersBinding
import com.familyplanner.databinding.FragmentTasksListBinding
import com.familyplanner.tasks.adapters.TaskAdapter
import com.familyplanner.tasks.model.Importance
import com.familyplanner.tasks.model.SortingType
import com.familyplanner.tasks.viewmodel.TasksListViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class TasksListFragment : Fragment() {
    private var _binding: FragmentTasksListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TasksListViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = FamilyPlanner.userId
        val familyId = requireArguments().getString("familyId")
        viewModel = ViewModelProvider(this)[TasksListViewModel::class.java]
        val tasksAdapter = TaskAdapter(viewModel::changeCompleted, userId, ::onTaskClicked, LocalDate.now().toEpochDay())
        binding.rvTasks.layoutManager = LinearLayoutManager(activity)
        binding.rvTasks.adapter = tasksAdapter
        viewModel.setUser(userId)
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTasks().collect {
                    activity?.runOnUiThread {
                        if (it.isEmpty()) {
                            binding.rvTasks.visibility = View.GONE
                            binding.ivNoTasks.visibility = View.VISIBLE
                            binding.tvNoTasks.visibility = View.VISIBLE
                        } else {
                            binding.rvTasks.visibility = View.VISIBLE
                            binding.ivNoTasks.visibility = View.GONE
                            binding.tvNoTasks.visibility = View.GONE
                        }
                        tasksAdapter.setTasks(it)
                    }
                }
            }
        }
        binding.ivTune.setOnClickListener {
            showFiltersBottomSheet()
        }
        binding.fabAdd.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("isPrivate", false)
            bundle.putString("parentId", null)
            bundle.putString("familyId", familyId)
            findNavController().navigate(
                R.id.action_tasksListFragment_to_newTaskInfoFragment,
                bundle
            )
        }
        binding.ivPerson.setOnClickListener {
            findNavController().navigate(R.id.action_tasksListFragment_to_profileFragment)
        }
        binding.tvDate.text = String.format(
            "%02d.%02d.%d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
        binding.tvDate.setOnClickListener {
            val dialog = DatePickerDialog(
                activity as MainActivity,
                R.style.datePickerDialog,
                { _, year, month, day ->
                    binding.tvDate.text = String.format("%02d.%02d.%d", day, month + 1, year)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, "Ок"
            ) { _, _ ->
                val newDate = LocalDate.of(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                viewModel.getTasksForDate(newDate)
                tasksAdapter.day = newDate.toEpochDay()
            }
            dialog.datePicker.minDate = calendar.timeInMillis
            dialog.show()
        }
    }

    private fun onTaskClicked(taskId: String) {
        findNavController().navigate(
            R.id.action_tasksListFragment_to_showTaskInfoFragment,
            bundleOf("taskId" to taskId)
        )
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { isGranted: Boolean ->
                    if (!isGranted) {
                        AlertDialog.Builder(requireContext()).setTitle("Важно")
                            .setMessage("Вы не будете получать уведомления о новых задачах")
                            .create().show()
                    }
                }
            }
        }
    }

    private fun showFiltersBottomSheet() {
        val bottomSheet = BottomSheetDialog(activity as MainActivity)
        val filtersBinding = BottomsheetTaskFiltersBinding.inflate(LayoutInflater.from(context))
        bottomSheet.setContentView(filtersBinding.root)
        bottomSheet.setOnDismissListener {
            viewModel.startFilterUpdate()
        }
        filtersBinding.sortImportanceAscending.setOnClickListener {
            val sortingType =
                if ((it as Chip).isChecked) SortingType.IMPORTANCE_ASC else SortingType.NONE
            viewModel.setSortingType(sortingType)
        }
        filtersBinding.sortImportanceDescending.setOnClickListener {
            val sortingType =
                if ((it as Chip).isChecked) SortingType.IMPORTANCE_DESC else SortingType.NONE
            viewModel.setSortingType(sortingType)
        }
        filtersBinding.sortDeadlineAscending.setOnClickListener {
            val sortingType =
                if ((it as Chip).isChecked) SortingType.DEADLINE_ASC else SortingType.NONE
            viewModel.setSortingType(sortingType)
        }
        filtersBinding.sortDeadlineDescending.setOnClickListener {
            val sortingType =
                if ((it as Chip).isChecked) SortingType.DEADLINE_DESC else SortingType.NONE
            viewModel.setSortingType(sortingType)
        }
        val userFilter = viewModel.getUserFilter()
        filtersBinding.usersFilter.removeAllViews()
        for (user in viewModel.getUsers()) {
            val chip = Chip(context)
            if (userFilter == user.id) {
                chip.isChecked = true
            }
            chip.setOnClickListener {
                viewModel.setUserFilter(if ((it as Chip).isChecked) user.id else null)
            }
            filtersBinding.usersFilter.addView(chip)
        }
        val importanceFilter = viewModel.getImportanceFilter()
        filtersBinding.importanceGroup.removeAllViews()
        for (value in Importance.values()) {
            val chip = Chip(context)
            if (value == importanceFilter) {
                chip.isChecked = true
            }
            chip.setOnClickListener {
                viewModel.setImportanceFilter(if ((it as Chip).isChecked) value else null)
            }
            filtersBinding.importanceGroup.addView(chip)
        }
        filtersBinding.hasDeadline.setOnClickListener {
            viewModel.setDeadlineFilter(if ((it as Chip).isChecked) true else null)
        }
        filtersBinding.noDeadline.setOnClickListener {
            viewModel.setDeadlineFilter(if ((it as Chip).isChecked) false else null)
        }
        filtersBinding.hasLocation.setOnClickListener {
            viewModel.setLocationFilter(if ((it as Chip).isChecked) true else null)
        }
        filtersBinding.noLocation.setOnClickListener {
            viewModel.setLocationFilter(if ((it as Chip).isChecked) false else null)
        }
        when (viewModel.getSortingType()) {
            SortingType.IMPORTANCE_ASC -> filtersBinding.sortImportanceAscending.isChecked =
                true

            SortingType.IMPORTANCE_DESC -> filtersBinding.sortImportanceDescending.isChecked =
                true

            SortingType.DEADLINE_ASC -> filtersBinding.sortDeadlineAscending.isChecked = true
            SortingType.DEADLINE_DESC -> filtersBinding.sortDeadlineDescending.isChecked = true
            SortingType.NONE -> filtersBinding.sortingGroup.clearCheck()
        }
        when (viewModel.getDeadlineFilter()) {
            true -> filtersBinding.hasDeadline.isChecked = true
            false -> filtersBinding.noDeadline.isChecked = true
            null -> filtersBinding.deadlineGroup.clearCheck()
        }
        when (viewModel.getLocationFilter()) {
            true -> filtersBinding.hasLocation.isChecked = true
            false -> filtersBinding.noLocation.isChecked = true
            null -> filtersBinding.locationGroup.clearCheck()
        }
        bottomSheet.behavior.isDraggable = false
        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}