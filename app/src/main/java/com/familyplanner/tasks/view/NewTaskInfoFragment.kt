package com.familyplanner.tasks.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentNewTaskBinding
import com.familyplanner.databinding.MapBinding
import com.familyplanner.tasks.FileAdapter
import com.familyplanner.tasks.Status
import com.familyplanner.tasks.viewmodel.NewTaskViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NewTaskInfoFragment : Fragment() {
    private var _binding: FragmentNewTaskBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private lateinit var viewModel: NewTaskViewModel
    private var bottomSheet: BottomSheetDialog? = null

    private val inputListener = object : InputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            lifecycleScope.launch(Dispatchers.IO) {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.getAddressByGeo(p1).collect {
                        if (it == Status.SUCCESS) {
                            activity?.runOnUiThread {
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

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val adapter = FileAdapter()
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFiles.layoutManager = layoutManager
        binding.rvFiles.adapter = adapter

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
            } else {
                binding.tvStartTime.visibility = View.GONE
                binding.tvFinishTime.visibility = View.GONE
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
            if (binding.tvRepeatStart.text.isNullOrBlank()) {
                binding.tvRepeatStart.text = String.format(
                    "%d.%02d.%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }
            binding.rbEachNDays.isChecked = false
            binding.llWeekdays.visibility = View.GONE
            binding.tvRepeatFrom.visibility = View.VISIBLE
            binding.tvRepeatStart.visibility = View.VISIBLE
            binding.etNumberOfDays.isEnabled = false
        }

        binding.rbEachNDays.setOnClickListener {
            if (binding.tvRepeatStart.text.isNullOrBlank()) {
                binding.tvRepeatStart.text = String.format(
                    "%d.%02d.%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
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
            binding.llWeekdays.visibility = View.VISIBLE
            binding.rbEachNDays.isChecked = false
            binding.tvRepeatFrom.visibility = View.VISIBLE
            binding.tvRepeatStart.visibility = View.VISIBLE
            binding.etNumberOfDays.isEnabled = true
            binding.tvRepeatStart.text = String.format(
                "%d.%02d.%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
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
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "file/*"
            startActivityForResult(intent, 1)
        }

        binding.ivNext.setOnClickListener {

        }
    }

    private fun setDeadline(calledBySwitch: Boolean = false) {
        val dialog = DatePickerDialog(
            activity as MainActivity,
            { _, year, month, day ->
                binding.tvDeadline.text = String.format("%d.%02d.%02d", year, month + 1, day)
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
            { _, year, month, day ->
                binding.tvRepeatStart.text = String.format("%d.%02d.%02d", year, month + 1, day)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = calendar.timeInMillis

        if (!binding.tvDeadline.text.isNullOrBlank()) {
            // TODO()
        }
        dialog.show()
    }

    private fun setTime(isStartTime: Boolean, calledByCheckbox: Boolean = false) {
        val dialog = TimePickerDialog(
            activity,
            { _, hour, minute ->
                if (isStartTime) {
                    binding.tvStartTime.text = String.format("%02d:%02d", hour, minute)

                    if (getTimeFromString(binding.tvFinishTime.text.toString()) < hour * 60 + minute) {
                        binding.tvFinishTime.text = String.format("%02d:%02d", hour, minute)
                    }
                } else {
                    binding.tvFinishTime.text = String.format("%02d:%02d", hour, minute)
                    if (getTimeFromString(binding.tvStartTime.text.toString()) > hour * 60 + minute) {
                        binding.tvStartTime.text = String.format("%02d:%02d", hour, minute)
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
        if (textTime.isNullOrBlank()) {
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