package com.familyplanner.events.view

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
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
import com.familyplanner.FamilyPlanner
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentNewEventBinding
import com.familyplanner.events.adapters.InvitationAdapter
import com.familyplanner.events.viewmodel.NewEventViewModel
import com.familyplanner.tasks.adapters.FileAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar

class NewEventFragment : Fragment() {
    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NewEventViewModel
    private val calendar = Calendar.getInstance()
    private lateinit var filesAdapter: FileAdapter
    private lateinit var attendeesAdapter: InvitationAdapter
    private val ATTACH_FILES = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NewEventViewModel::class.java]
        val filesLayoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        filesAdapter = FileAdapter()
        binding.rvFiles.layoutManager = filesLayoutManager
        binding.rvFiles.adapter = filesAdapter
        val attendeesLayoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        attendeesAdapter = InvitationAdapter()
        binding.rvObservers.layoutManager = attendeesLayoutManager
        binding.rvObservers.adapter = attendeesAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getAttendees().collect {
                    requireActivity().runOnUiThread {
                        if (it.isNotEmpty()) {
                            attendeesAdapter.setData(it)
                        }
                    }
                }
            }
        }

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        val defaultDate = Calendar.getInstance()
        defaultDate.add(Calendar.HOUR_OF_DAY, 1)
        binding.tvStartValue.text = String.format(
            "%02d.%02d.%d %02d:%02d",
            defaultDate.get(Calendar.DAY_OF_MONTH),
            defaultDate.get(Calendar.MONTH) + 1,
            defaultDate.get(Calendar.YEAR),
            defaultDate.get(Calendar.HOUR_OF_DAY),
            defaultDate.get(Calendar.MINUTE)
        )
        binding.tvFinishValue.text = String.format(
            "%02d.%02d.%d %02d:%02d",
            defaultDate.get(Calendar.DAY_OF_MONTH),
            defaultDate.get(Calendar.MONTH) + 1,
            defaultDate.get(Calendar.YEAR),
            defaultDate.get(Calendar.HOUR_OF_DAY),
            defaultDate.get(Calendar.MINUTE)
        )
        binding.tvStartValue.setOnClickListener {
            setDate(true)
        }
        binding.tvFinishValue.setOnClickListener {
            setDate(false)
        }
        binding.tvAttachFile.setOnClickListener {
            val openDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
            }
            startActivityForResult(openDocumentIntent, ATTACH_FILES)
        }
        binding.ivDone.setOnClickListener {
            if (binding.etName.text.isNullOrBlank()) {
                binding.tfName.error = "Название мероприятия не может быть пустым"
            }
            binding.tfName.isErrorEnabled = false
            viewModel.createEvent(
                binding.etName.text!!.toString().trim(),
                binding.etDescription.text!!.toString().trim(),
                getDateTimeFromString(binding.tvStartTime.text.toString()),
                getDateTimeFromString(binding.tvFinishTime.text.toString()),
                attendeesAdapter.getInvitations(),
                filesAdapter.getFiles()
            )
        }
    }

    private fun getDateTimeFromString(value: String): Long {
        return LocalDateTime.parse(value, FamilyPlanner.dadteTimeFormatter)
            .atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
            .toInstant().epochSecond
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
                    val sizeIndex = cursor!!.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    val name = cursor.getString(nameIndex)
                    val size = cursor.getDouble(sizeIndex)
                    filesAdapter.addFile(uri, name, size)
                }
            }
        }
    }

    private fun setDate(isStartTime: Boolean) {
        val dialog = DatePickerDialog(
            activity as MainActivity,
            R.style.datePickerDialog,
            { _, year, month, day ->
                setTime(isStartTime, day, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = calendar.timeInMillis
        dialog.show()
    }

    private fun setTime(isStartTime: Boolean, day: Int, month: Int, year: Int) {
        val dialog = TimePickerDialog(
            activity,
            R.style.datePickerDialog,
            { _, chosenHour, chosenMinute ->
                var hour = chosenHour
                var minute = chosenMinute
                val today = Calendar.getInstance()
                if (isToday(day, month, year))
                    if (hour < today.get(Calendar.HOUR_OF_DAY) || minute < today.get(Calendar.MINUTE)) {
                        hour = today.get(Calendar.HOUR_OF_DAY)
                        minute = today.get(Calendar.MINUTE)
                    }
                if (isStartTime) {
                    binding.tvStartValue.text =
                        String.format("%02d.%02d.%d %02d:%02d", day, month, year, hour, minute)
                    if (binding.tvFinishTime.text.isNullOrBlank() || getTimeFromString(
                            binding.tvFinishValue.text.toString().split(' ')[1]
                        ) < hour * 60 + minute
                    ) {
                        binding.tvFinishValue.text =
                            String.format("%02d.%02d.%d %02d:%02d", day, month, year, hour, minute)
                    }
                } else {
                    binding.tvFinishValue.text =
                        String.format("%02d.%02d.%d %02d:%02d", day, month, year, hour, minute)
                    if (binding.tvStartValue.text.isNullOrBlank() || getTimeFromString(
                            binding.tvStartValue.text.toString().split(' ')[1]
                        ) > hour * 60 + minute
                    ) {
                        binding.tvStartValue.text =
                            String.format("%02d.%02d.%d %02d:%02d", day, month, year, hour, minute)
                    }
                }
            },
            12,
            0,
            true
        )
        dialog.show()
    }

    private fun isToday(day: Int, month: Int, year: Int): Boolean {
        val today = Calendar.getInstance()
        return year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH) && day == today.get(
            Calendar.DAY_OF_MONTH
        )
    }

    private fun getTimeFromString(textTime: String): Int {
        if (textTime.isBlank()) {
            return -1
        }

        val parts = textTime.split(':').map { it.toInt() }
        return parts[0] * 60 + parts[1]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}