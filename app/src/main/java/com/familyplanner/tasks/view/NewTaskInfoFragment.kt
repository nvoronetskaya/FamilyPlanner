package com.familyplanner.tasks.view

import android.app.Activity.RESULT_OK
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
import com.familyplanner.databinding.FragmentNewTaskBinding
import com.familyplanner.tasks.adapters.FileAdapter
import com.familyplanner.tasks.data.Importance
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.SizeExceededException
import com.familyplanner.tasks.data.Status
import com.familyplanner.tasks.data.TaskCreationStatus
import com.familyplanner.tasks.data.UserFile
import com.familyplanner.tasks.viewmodel.NewTaskViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar

class NewTaskInfoFragment : Fragment() {
    private var _binding: FragmentNewTaskBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private lateinit var viewModel: NewTaskViewModel
    private var bottomSheet: BottomSheetDialog? = null
    private val ATTACH_FILES = 10
    private lateinit var filesAdapter: FileAdapter
    private var curPoint: Point? = null
    private lateinit var userId: String
    private lateinit var familyId: String
    private var parentId: String? = null
    private lateinit var imageProvider: ImageProvider

    private val inputListener = object : InputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
                Toast.makeText(
                    activity,
                    "Ошибка. Проверьте подключение к сети и повторите позднее",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            lifecycleScope.launch(Dispatchers.IO) {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.getAddressByGeo(p1).collect {
                        activity?.runOnUiThread {
                            if (it == Status.SUCCESS) {
                                if (_binding == null) {
                                    return@runOnUiThread
                                }
                                curPoint = p1
                                binding.tvAddress.visibility = View.VISIBLE
                                binding.tvAddress.text = viewModel.getAddress()
                                bottomSheet?.cancel()
                            } else {
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
    ): View? {
        _binding = FragmentNewTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NewTaskViewModel::class.java]
        imageProvider = ImageProvider.fromResource(requireContext(), R.drawable.map_mark)
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val adapter = FileAdapter()
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFiles.layoutManager = layoutManager
        binding.rvFiles.adapter = adapter
        this.filesAdapter = adapter

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

        binding.tvStartValue.setOnClickListener {
            setTime(isStartTime = true, calledByCheckbox = false)
        }

        binding.tvFinishValue.setOnClickListener {
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

        binding.ivNext.setOnClickListener {
            createTask()
        }
        val options = listOf("Низкая", "Средняя", "Высокая")
        binding.spImportance.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        binding.spImportance.setSelection(0)
        userId = FamilyPlanner.userId
        familyId = requireArguments().getString("familyId")!!
        parentId = requireArguments().getString("parentId")

        lifecycleScope.launch(Dispatchers.IO) {
            val creationResult = viewModel.getCreationStatus()
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                creationResult.collect {
                    requireActivity().runOnUiThread {
                        when (it) {
                            TaskCreationStatus.SUCCESS -> if (parentId != null) {
                                findNavController().popBackStack()
                            } else {
                                val bundle = Bundle()
                                bundle.putString("taskId", viewModel.getCreatedTaskId())
                                bundle.putString("familyId", familyId)
                                findNavController().navigate(
                                    R.id.action_newTaskInfoFragment_to_newTaskObserversFragment,
                                    bundle
                                )
                            }

                            TaskCreationStatus.FILE_UPLOAD_FAILED -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось прикрепить некоторые файлы. Вы можете отредактировать задачу позднее",
                                    Toast.LENGTH_LONG
                                ).show()
                                val bundle = Bundle()
                                bundle.putString("taskId", viewModel.getCreatedTaskId())
                                bundle.putString("familyId", familyId)
                                findNavController().navigate(
                                    R.id.action_newTaskInfoFragment_to_newTaskObserversFragment,
                                    bundle
                                )
                            }

                            TaskCreationStatus.FAILED -> Toast.makeText(
                                requireContext(),
                                "Ошибка. Проверьте подключение к сети и повторите позднее",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ATTACH_FILES) {
            if (resultCode == RESULT_OK) {
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
                        Toast.makeText(
                            requireContext(),
                            "Файл с таким именем уже добавлен",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: SizeExceededException) {
                        Toast.makeText(
                            requireContext(),
                            "Суммарный размер файлов не может превышать 5 МБ",
                            Toast.LENGTH_SHORT
                        ).show()
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

                    if (binding.tvFinishTime.text.isNullOrBlank()) {
                        binding.tvFinishValue.text = String.format("%02d:%02d", hour, minute)
                    }
                } else {
                    binding.tvFinishValue.text = String.format("%02d:%02d", hour, minute)
                    if (binding.tvStartValue.text.isNullOrBlank()) {
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
        if (curPoint != null) {
            map.move(CameraPosition(curPoint!!, 15f, 0f, 0f))
            map.mapObjects.addPlacemark().apply {
                geometry = curPoint!!
                setIcon(imageProvider)
            }
        }
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

    private fun createTask() {
        if (binding.etName.text.isNullOrBlank()) {
            binding.etName.error = "Текст задачи не может быть пустым"
            return
        }
        binding.etName.error = null
        var weekDays = 0
        var type: RepeatType
        var eachNDays = 0
        binding.etNumberOfDays.error = null
        if (binding.rbOnce.isChecked) {
            type = RepeatType.ONCE
        } else if (binding.rbEveryDay.isChecked) {
            type = RepeatType.EVERY_DAY
        } else if (binding.rbEachNDays.isChecked) {
            if (binding.etNumberOfDays.text.isNullOrBlank()) {
                binding.etNumberOfDays.error = "Введите число дней"
                return
            }
            type = RepeatType.EACH_N_DAYS
            eachNDays = binding.etNumberOfDays.text.toString().toInt()
            if (eachNDays < 2 || eachNDays > 30) {
                binding.etNumberOfDays.error = "Введите число от 2 до 30"
                return
            }
        } else if (binding.rbDaysOfWeek.isChecked) {
            val iterator = binding.llWeekdays.children.iterator()
            var power = 1
            while (iterator.hasNext()) {
                if ((iterator.next() as CheckBox).isChecked) {
                    weekDays += power
                }
                power *= 2
            }
            if (weekDays == 0) {
                binding.tvRepeat.error = "Выберите хотя бы один день недели"
                return
            }
            type = RepeatType.DAYS_OF_WEEK
        } else {
            binding.tvRepeat.error = "Выберите тип повтора задачи"
            return
        }
        binding.tvRepeat.error = null
        val startTime: Int
        val finishTime: Int
        if (binding.cbContinuousTask.isChecked) {
            val zonedStartTime = getTimeFromString(binding.tvStartValue.text.trim().toString())
            val zonedFinishTime = getTimeFromString(binding.tvFinishValue.text.trim().toString())
            val startDateTime =
                LocalDateTime.now().atZone(ZoneId.systemDefault()).withHour(zonedStartTime / 60)
                    .withMinute(zonedStartTime % 60).withZoneSameInstant(ZoneOffset.UTC)
            val finishDateTime =
                LocalDateTime.now().atZone(ZoneId.systemDefault()).withHour(zonedFinishTime / 60)
                    .withMinute(zonedFinishTime % 60).withZoneSameInstant(ZoneOffset.UTC)
            startTime = startDateTime.hour * 60 + startDateTime.minute
            finishTime = finishDateTime.hour * 60 + finishDateTime.minute
        } else {
            startTime = 0
            finishTime = 0
        }
        viewModel.createTask(
            binding.etName.text!!.trim().toString(),
            if (binding.swDeadline.isChecked) LocalDate.parse(
                binding.tvDeadline.text.trim().toString(), FamilyPlanner.uiDateFormatter
            ).toEpochDay() else null,
            binding.cbContinuousTask.isChecked,
            startTime,
            finishTime,
            type,
            eachNDays,
            weekDays,
            if (type != RepeatType.ONCE) LocalDate.parse(
                binding.tvRepeatStart.text.trim().toString(), FamilyPlanner.uiDateFormatter
            ).toEpochDay() else null,
            Importance.values()[binding.spImportance.selectedItemPosition],
            curPoint,
            if (curPoint != null) binding.tvAddress.text.toString() else null,
            userId,
            familyId,
            filesAdapter.getFiles(),
            parentId,
            (requireActivity() as MainActivity).isConnectedToInternet()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            val creationResult = viewModel.getCreationStatus()
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                creationResult.collect {
                    requireActivity().runOnUiThread {
                        when (it) {
                            TaskCreationStatus.SUCCESS -> if (parentId != null) {
                                findNavController().popBackStack()
                            } else {
                                val bundle = Bundle()
                                bundle.putString("taskId", viewModel.getCreatedTaskId())
                                bundle.putString("familyId", familyId)
                                findNavController().navigate(
                                    R.id.action_newTaskInfoFragment_to_newTaskObserversFragment,
                                    bundle
                                )
                            }

                            TaskCreationStatus.FILE_UPLOAD_FAILED -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось прикрепить некоторые файлы. Вы можете отредактировать задачу позднее",
                                    Toast.LENGTH_LONG
                                ).show()
                                val bundle = Bundle()
                                bundle.putString("taskId", viewModel.getCreatedTaskId())
                                bundle.putString("familyId", familyId)
                                findNavController().navigate(
                                    R.id.action_newTaskInfoFragment_to_newTaskObserversFragment,
                                    bundle
                                )
                            }

                            TaskCreationStatus.FAILED -> Toast.makeText(
                                requireContext(),
                                "Ошибка. Проверьте подключение к сети и повторите позднее",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}