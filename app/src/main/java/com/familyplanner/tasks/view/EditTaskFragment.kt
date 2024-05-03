package com.familyplanner.tasks.view

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.view.children
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
import com.familyplanner.databinding.FragmentNewTaskBinding
import com.familyplanner.events.adapters.InvitationAdapter
import com.familyplanner.tasks.adapters.FileAdapter
import com.familyplanner.tasks.model.Importance
import com.familyplanner.tasks.model.RepeatType
import com.familyplanner.tasks.model.Status
import com.familyplanner.tasks.model.TaskCreationStatus
import com.familyplanner.tasks.model.UserFile
import com.familyplanner.tasks.viewmodel.EditTaskViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class EditTaskFragment : Fragment() {
    private var _binding: FragmentNewTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EditTaskViewModel
    private var calendar: Calendar = Calendar.getInstance()
    private var bottomSheet: BottomSheetDialog? = null
    private val ATTACH_FILES = 10
    private lateinit var filesAdapter: FileAdapter
    private var curPoint: Point? = null
    private lateinit var userId: String
    private lateinit var familyId: String
    private var parentId: String? = null

    private val inputListener = object : InputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            lifecycleScope.launch(Dispatchers.IO) {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.getAddressByGeo(p1).collect {
                        if (it == Status.SUCCESS) {
                            activity?.runOnUiThread {
                                curPoint = p1
                                binding.tvAddress.visibility = View.VISIBLE
                                binding.tvAddress.text = viewModel.getAddress()
                                bottomSheet?.cancel()
                            }
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Ошибка. Проверьте подключение к сети и повторите позднее",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        override fun onMapLongTap(p0: Map, p1: Point) {
        }
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[EditTaskViewModel::class.java]
        val taskId = requireArguments().getString("taskId") ?: ""
        val options = listOf("Низкая", "Средняя", "Высокая")
        filesAdapter = FileAdapter(viewModel::removeFile)
        binding.rvFiles.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFiles.adapter = filesAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            val task = viewModel.getTask(taskId)
            requireActivity().runOnUiThread {
                if (task == null) {
                    Toast.makeText(requireContext(), "Мероприятие недоступно", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().popBackStack()
                    return@runOnUiThread
                }
                binding.etName.setText(task.title)
                if (task.deadline != null) {
                    binding.swDeadline.isChecked = true
                    binding.tvDeadline.isVisible = true
                    binding.tvDeadline.text =
                        LocalDate.ofEpochDay(task.deadline!!).format(FamilyPlanner.uiDateFormatter)
                } else {
                    binding.swDeadline.isChecked = false
                    binding.tvDeadline.isVisible = false
                }
                binding.cbContinuousTask.isChecked = task.isContinuous
                when (task.repeatType) {
                    RepeatType.ONCE -> binding.rbOnce.isChecked = true
                    RepeatType.EVERY_DAY -> binding.rbEveryDay.isChecked = true
                    RepeatType.EACH_N_DAYS -> {
                        binding.rbEachNDays.isChecked = true
                        binding.etNumberOfDays.setText(task.nDays.toString())
                    }

                    RepeatType.DAYS_OF_WEEK -> {
                        binding.rbDaysOfWeek.isChecked = true
                        val iterator = binding.llWeekdays.children.iterator()
                        var power = 1
                        while (iterator.hasNext()) {
                            (iterator.next() as CheckBox).isChecked = power and task.daysOfWeek > 0
                            power *= 2
                        }
                    }
                }
                if (task.repeatStart != null) {
                    binding.tvRepeatStart.isVisible = true
                    binding.tvRepeatStart.text = LocalDate.ofEpochDay(task.repeatStart!!)
                        .format(FamilyPlanner.uiDateFormatter)
                } else {
                    binding.tvRepeatFrom.isVisible = false
                    binding.tvRepeatStart.isVisible = false
                }
                binding.spImportance.adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
                binding.spImportance.setSelection(task.importance.ordinal)
                binding.swHasLocation.isChecked = task.location != null
                binding.tvAddress.isVisible = task.location != null
            }
        }
        addListeners()
        binding.ivNext.setOnClickListener {
            updateTask()
        }
    }

    private fun addListeners() {
        binding.swDeadline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setDeadline(true)
                binding.tvDeadline.visibility = View.VISIBLE
            } else {
                binding.tvDeadline.visibility = View.GONE
            }
        }

        binding.tvDeadline.setOnClickListener {
            setDeadline()
        }

        binding.cbContinuousTask.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setTime(isStartTime = true, calledByCheckbox = true)
                binding.tvStartTime.visibility = View.VISIBLE
                binding.tvFinishTime.visibility = View.VISIBLE
                binding.tvStartValue.visibility = View.VISIBLE
                binding.tvFinishValue.visibility = View.VISIBLE
            } else {
                binding.tvStartTime.visibility = View.GONE
                binding.tvFinishTime.visibility = View.GONE
                binding.tvStartValue.visibility = View.GONE
                binding.tvFinishValue.visibility = View.GONE
            }
        }

        binding.tvStartTime.setOnClickListener {
            setTime(isStartTime = true, calledByCheckbox = false)
        }

        binding.tvFinishTime.setOnClickListener {
            setTime(isStartTime = false, calledByCheckbox = false)
        }

        binding.rbOnce.setOnClickListener {
            binding.tvRepeatFrom.visibility = View.GONE
            binding.rbEachNDays.isChecked = false
            binding.llWeekdays.visibility = View.GONE
            binding.etNumberOfDays.isEnabled = false
            binding.tvRepeatStart.visibility = View.GONE
        }

        binding.rbEveryDay.setOnClickListener {
            binding.swHasLocation.isChecked = false
            if (binding.tvRepeatStart.text.isNullOrBlank()) {
                binding.tvRepeatStart.text = String.format(
                    "%02d.%02d.%d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.YEAR)
                )
            }
            binding.rbEachNDays.isChecked = false
            binding.llWeekdays.visibility = View.GONE
            binding.tvRepeatFrom.visibility = View.VISIBLE
            binding.tvRepeatStart.visibility = View.VISIBLE
            binding.etNumberOfDays.isEnabled = false
        }

        binding.rbEachNDays.setOnClickListener {
            binding.swHasLocation.isChecked = false
            if (binding.tvRepeatStart.text.isNullOrBlank()) {
                binding.tvRepeatStart.text = String.format(
                    "%02d.%02d.%d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.YEAR)
                )
            }
            binding.rbEveryDay.isChecked = false
            binding.rbOnce.isChecked = false
            binding.rbDaysOfWeek.isChecked = false
            binding.llWeekdays.visibility = View.GONE
            binding.tvRepeatFrom.visibility = View.VISIBLE
            binding.tvRepeatStart.visibility = View.VISIBLE
            binding.etNumberOfDays.isEnabled = true
        }

        binding.rbDaysOfWeek.setOnClickListener {
            binding.swHasLocation.isChecked = false
            binding.llWeekdays.visibility = View.VISIBLE
            binding.rbEachNDays.isChecked = false
            binding.tvRepeatFrom.visibility = View.VISIBLE
            binding.tvRepeatStart.visibility = View.VISIBLE
            binding.etNumberOfDays.isEnabled = true
            binding.tvRepeatStart.text = String.format(
                "%02d.%02d.%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)
            )
        }

        binding.swHasLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvAddress.visibility = View.VISIBLE
                showBottomSheet()
            } else {
                binding.tvAddress.visibility = View.GONE
            }
        }

        binding.tvAddress.setOnClickListener {
            showBottomSheet()
        }

        binding.tvRepeatStart.setOnClickListener {
            setStartDate()
        }

        binding.tvAttachFile.setOnClickListener {
            val openDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
            }
            startActivityForResult(openDocumentIntent, ATTACH_FILES)
        }
    }

    private fun updateTask() {
        if (binding.etName.text.isNullOrBlank()) {
            binding.etName.error = "Текст задачи не может быть пустым"
            return
        }
        var weekDays = 0
        val type: RepeatType
        var eachNDays = 0
        if (binding.rbOnce.isChecked) {
            type = RepeatType.ONCE
        } else if (binding.rbEveryDay.isChecked) {
            type = RepeatType.EVERY_DAY
        } else if (binding.rbEachNDays.isChecked) {
            if (binding.etNumberOfDays.text.isNullOrBlank()) {
                return
            }
            type = RepeatType.EACH_N_DAYS
            eachNDays = binding.etNumberOfDays.text.toString().toInt()
        } else {
            val iterator = binding.llWeekdays.children.iterator()
            var power = 1
            while (iterator.hasNext()) {
                if ((iterator.next() as CheckBox).isChecked) {
                    weekDays += power
                }
                power *= 2
            }
            if (weekDays == 0) {
                binding.rbDaysOfWeek.error = "Выберите хотя бы один день недели"
                return
            }
            type = RepeatType.DAYS_OF_WEEK
        }

        viewModel.updateTask(
            binding.etName.text!!.trim().toString(),
            binding.swDeadline.isChecked,
            if (binding.swDeadline.isChecked) LocalDate.parse(
                binding.tvDeadline.text.trim().toString(), FamilyPlanner.uiDateFormatter
            ).toEpochDay() else null,
            binding.cbContinuousTask.isChecked,
            if (binding.cbContinuousTask.isChecked) getTimeFromString(
                binding.tvStartValue.text.trim().toString()
            ) else 0,
            if (binding.cbContinuousTask.isChecked) getTimeFromString(
                binding.tvFinishValue.text.trim().toString()
            ) else 0,
            type,
            eachNDays,
            weekDays,
            if (type != RepeatType.ONCE) LocalDate.parse(
                binding.tvRepeatStart.text.trim().toString(), FamilyPlanner.uiDateFormatter
            ).toEpochDay() else null,
            Importance.values()[binding.spImportance.selectedItemPosition],
            binding.swHasLocation.isChecked,
            curPoint
        )
        findNavController().popBackStack()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ATTACH_FILES) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                if (uri == null) {
                    Toast.makeText(activity, "Не удалось прикрепить файл", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                requireActivity().contentResolver.query(uri, null, null, null, null).use { cursor ->
                    val nameIndex = cursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    val name = cursor.getString(nameIndex)
                    val size = cursor.getDouble(sizeIndex)
                    try {
                        filesAdapter.addFile(UserFile(uri, name, size))
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(requireContext(), "Файл с таким именем уже добавлен", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setDeadline(calledBySwitch: Boolean = false) {
        val dialog = DatePickerDialog(
            activity as MainActivity,
            R.style.datePickerDialog,
            { _, year, month, day ->
                binding.tvDeadline.text = String.format("%02d.%02d.%d", day, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnCancelListener {
            if (calledBySwitch) {
                binding.swDeadline.isChecked = false
            }
        }
        dialog.datePicker.minDate = calendar.timeInMillis
        dialog.show()
    }

    private fun setStartDate() {
        val dialog = DatePickerDialog(
            activity as MainActivity,
            R.style.datePickerDialog,
            { _, year, month, day ->
                binding.tvRepeatStart.text = String.format("%02d.%02d.%d", day, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = calendar.timeInMillis
        dialog.show()
    }

    private fun setTime(isStartTime: Boolean, calledByCheckbox: Boolean = false) {
        val dialog = TimePickerDialog(
            activity,
            R.style.datePickerDialog,
            { _, hour, minute ->
                if (isStartTime) {
                    binding.tvStartValue.text = String.format("%02d:%02d", hour, minute)

                    if (binding.tvFinishTime.text.isNullOrBlank() || getTimeFromString(binding.tvFinishValue.text.toString()) < hour * 60 + minute) {
                        binding.tvFinishValue.text = String.format("%02d:%02d", hour, minute)
                    }
                } else {
                    binding.tvFinishValue.text = String.format("%02d:%02d", hour, minute)
                    if (binding.tvStartValue.text.isNullOrBlank() || getTimeFromString(binding.tvStartValue.text.toString()) > hour * 60 + minute) {
                        binding.tvStartValue.text = String.format("%02d:%02d", hour, minute)
                    }
                }

                if (calledByCheckbox && isStartTime) {
                    setTime(isStartTime = false, calledByCheckbox = true)
                }
            },
            12,
            0,
            true
        )
        dialog.setOnCancelListener {
            if (calledByCheckbox) {
                binding.cbContinuousTask.isChecked = false
            }
        }
        dialog.show()
    }

    private fun getTimeFromString(textTime: String): Int {
        if (textTime.isBlank()) {
            return -1
        }

        val parts = textTime.split(':').map { it.toInt() }
        return parts[0] * 60 + parts[1]
    }

    private fun showBottomSheet() {
        val bottomSheet = BottomSheetDialog(activity as MainActivity)
        bottomSheet.setContentView(R.layout.bottomsheet_map)
        val map = bottomSheet.findViewById<MapView>(R.id.map)?.mapWindow?.map ?: return

        map.addInputListener(inputListener)
        bottomSheet.setOnDismissListener {
            if (binding.tvAddress.text.isNullOrBlank()) {
                binding.swHasLocation.isChecked = false
            }
        }

        bottomSheet.behavior.isDraggable = false
        this.bottomSheet = bottomSheet
        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}