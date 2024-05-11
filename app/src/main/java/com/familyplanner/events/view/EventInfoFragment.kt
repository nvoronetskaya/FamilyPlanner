package com.familyplanner.events.view

import android.app.DownloadManager
import android.app.Service
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentEventInfoBinding
import com.familyplanner.events.adapters.AttendeeAdapter
import com.familyplanner.events.data.EventAttendeeStatus
import com.familyplanner.events.viewmodel.EventInfoViewModel
import com.familyplanner.tasks.adapters.ObserveFilesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EventInfoFragment : Fragment() {
    private var _binding: FragmentEventInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EventInfoViewModel
    private lateinit var eventId: String
    private lateinit var dividerItemDecoration: DividerItemDecoration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[EventInfoViewModel::class.java]
        eventId = requireArguments().getString("eventId")!!
        viewModel.setEvent(eventId)
        val attendeeAdapter = AttendeeAdapter()
        val attendeeLayoutManager = LinearLayoutManager(context)
        binding.rvObservers.layoutManager = attendeeLayoutManager
        binding.rvObservers.adapter = attendeeAdapter
        val filesAdapter = ObserveFilesAdapter(::downloadFile)
        binding.rvFiles.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFiles.adapter = filesAdapter
        dividerItemDecoration = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.divider
            )!!
        )
        binding.rvObservers.addItemDecoration(dividerItemDecoration)
        if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
            Toast.makeText(
                requireContext(),
                "Нет сети. Файлы недоступны ",
                Toast.LENGTH_SHORT
            ).show()
            binding.llFiles.isVisible = false
        }
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getEvent().collect {
                        requireActivity().runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            if (it == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Мероприятие не найдено",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().popBackStack()
                            } else {
                                binding.etName.setText(it.name)
                                binding.tfDescription.isVisible = it.description.isNotBlank()
                                binding.etDescription.setText(it.description)
                                binding.tvStartValue.text = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(it.start),
                                    ZoneId.systemDefault()
                                ).format(FamilyPlanner.dateTimeFormatter)
                                binding.tvFinishValue.text = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(it.start),
                                    ZoneId.systemDefault()
                                ).format(FamilyPlanner.dateTimeFormatter)
                                binding.ivEdit.isVisible = FamilyPlanner.userId.equals(it.createdBy)
                                binding.tvCancel.isVisible =
                                    FamilyPlanner.userId.equals(it.createdBy)
                            }
                        }
                    }
                }
                launch {
                    viewModel.getAttendees().collect {
                        requireActivity().runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            attendeeAdapter.setData(it)
                            val curUser =
                                it.firstOrNull { attendee -> attendee.userId.equals(FamilyPlanner.userId) }
                            curUser?.let {
                                if (it.status == EventAttendeeStatus.UNKNOWN) {
                                    viewModel.changeComing(
                                        FamilyPlanner.userId,
                                        eventId,
                                        binding.cbParticipate.isChecked
                                    )
                                }
                                binding.cbParticipate.isChecked =
                                    it.status == EventAttendeeStatus.COMING
                            }
                        }
                    }
                }
                launch {
                    viewModel.getFiles().collect {
                        requireActivity().runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            binding.llFiles.isVisible = it.isNotEmpty()
                            filesAdapter.addPaths(it)
                        }
                    }
                }
            }
        }
        binding.ivEdit.setOnClickListener {
            if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
                Toast.makeText(
                    requireContext(),
                    "Нет сети. Проверьте подключение и попробуйте позднее ",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val bundle = Bundle()
            bundle.putString("eventId", eventId)
            bundle.putString("familyId", viewModel.getFamilyId())
            findNavController().navigate(
                R.id.action_eventInfoFragment_to_editEventFragment,
                bundle
            )
        }
        binding.tvCancel.setOnClickListener {
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Отмена мероприятия")
                .setMessage("Вы уверены, что хотите отменить мероприятие?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.deleteEvent(eventId)
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }
        binding.cbParticipate.setOnClickListener {
            viewModel.changeComing(
                FamilyPlanner.userId,
                eventId,
                binding.cbParticipate.isChecked
            )
        }
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.ivObserversFolded.setOnClickListener {
            binding.rvObservers.isVisible = true
            binding.ivObserversFolded.isVisible = false
            binding.ivObserversUnfolded.isVisible = true
        }
        binding.ivObserversUnfolded.setOnClickListener {
            binding.rvObservers.isVisible = false
            binding.ivObserversFolded.isVisible = true
            binding.ivObserversUnfolded.isVisible = false
        }
    }

    private fun downloadFile(path: String) {
        if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
            Toast.makeText(
                requireContext(),
                "Нет сети. Файлы недоступны ",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val request = DownloadManager.Request(viewModel.downloadFile(eventId, path))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                path
            )
        val downloadManager =
            requireContext().getSystemService(Service.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Файл загружается... ", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}