package com.familyplanner.tasks.view

import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
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
import com.familyplanner.databinding.FragmentTaskInfoBinding
import com.familyplanner.tasks.adapters.CommentsListAdapter
import com.familyplanner.tasks.adapters.FileAdapter
import com.familyplanner.tasks.adapters.ObserveFilesAdapter
import com.familyplanner.tasks.adapters.ObserversListAdapter
import com.familyplanner.tasks.adapters.TaskAdapter
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.Task
import com.familyplanner.tasks.data.TaskCreationStatus
import com.familyplanner.tasks.data.UserFile
import com.familyplanner.tasks.viewmodel.TaskInfoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ShowTaskInfoFragment : Fragment() {
    private var _binding: FragmentTaskInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TaskInfoViewModel
    private lateinit var userId: String
    private val ATTACH_FILES = 10
    private lateinit var taskId: String
    private lateinit var commentFilesAdapter: FileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId = requireArguments().getString("taskId")!!
        userId = FamilyPlanner.userId
        viewModel = ViewModelProvider(this)[TaskInfoViewModel::class.java]
        viewModel.setTask(taskId)

        var task: Task? = null
        val commentsAdapter = CommentsListAdapter(::downloadCommentFile, userId)
        val observersAdapter = ObserversListAdapter(userId)
        val subtasksAdapter = TaskAdapter(
            viewModel::changeCompleted,
            userId,
            ::onTaskClicked,
            LocalDate.now().toEpochDay()
        )
        val filesAdapter = ObserveFilesAdapter(::downloadTaskFile)
        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentsAdapter
        binding.rvObservers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObservers.adapter = observersAdapter
        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = filesAdapter
        commentFilesAdapter = FileAdapter()
        binding.rvCommentFiles.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCommentFiles.adapter = commentFilesAdapter
        if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
            Toast.makeText(requireContext(), "Ошибка сети. Файлы и комментарии недоступны", Toast.LENGTH_SHORT).show()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getTask().collect {
                        requireActivity().runOnUiThread {
                            if (it == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Задача недоступна",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().popBackStack()
                                return@runOnUiThread
                            }
                            task = it
                            bindTask(it)
                        }
                    }
                }
                launch {
                    viewModel.getComments().collect {
                        requireActivity().runOnUiThread {
                            commentsAdapter.setComments(it)
                        }
                    }
                }
                launch {
                    viewModel.getObservers().collect {
                        requireActivity().runOnUiThread {
                            observersAdapter.setObservers(it)
                        }
                    }
                }
                launch {
                    viewModel.getSubtasks().collect {
                        requireActivity().runOnUiThread {
                            subtasksAdapter.setTasks(it)
                            binding.layoutSubtasks.isVisible = it.isNotEmpty()
                        }
                    }
                }
                launch {
                    viewModel.getFiles().collect {
                        requireActivity().runOnUiThread {
                            if (it == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось получить файлы. Проверьте соединение",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.layoutFiles.isVisible = false
                                return@runOnUiThread
                            }
                            binding.layoutFiles.isVisible = it.isNotEmpty()
                            filesAdapter.addPaths(it)
                        }
                    }
                }
                launch {
                    val creationStatus = viewModel.getCreationStatus()
                    creationStatus.collect {
                        requireActivity().runOnUiThread {
                            when (it) {
                                TaskCreationStatus.SUCCESS -> {
                                    commentFilesAdapter.clearFiles()
                                    binding.etComment.text?.clear()
                                }

                                TaskCreationStatus.FILE_UPLOAD_FAILED -> {
                                    Toast.makeText(
                                        requireContext(),
                                        "Не удалось прикрепить некоторые файлы",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    commentFilesAdapter.clearFiles()
                                    binding.etComment.text?.clear()
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
                launch {
                    viewModel.getCurObserver().collect {
                        requireActivity().runOnUiThread {
                            if (it == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Задача недоступна",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                binding.cbBecomeExecutor.isChecked = it.isExecutor
                            }
                        }
                    }
                }
            }
        }
        binding.ivAddSubtask.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("parentId", taskId)
            bundle.putString("familyId", task!!.familyId)
            bundle.putString("userId", userId)
            findNavController().navigate(R.id.action_showTaskInfoFragment_to_newTaskInfoFragment)
        }
        binding.ivSubtasksUnfolded.setOnClickListener {
            binding.ivSubtasksUnfolded.visibility = View.GONE
            binding.ivSubtasksFolded.visibility = View.VISIBLE
            binding.rvSubtasks.visibility = View.GONE
        }
        binding.ivSubtasksFolded.setOnClickListener {
            binding.ivSubtasksFolded.visibility = View.GONE
            binding.ivSubtasksUnfolded.visibility = View.VISIBLE
            binding.rvSubtasks.visibility = View.VISIBLE
        }
        binding.ivObserversUnfolded.setOnClickListener {
            binding.ivObserversUnfolded.visibility = View.GONE
            binding.ivObserversFolded.visibility = View.VISIBLE
            binding.rvObservers.visibility = View.GONE
        }
        binding.ivObserversFolded.setOnClickListener {
            binding.ivObserversFolded.visibility = View.GONE
            binding.ivObserversUnfolded.visibility = View.VISIBLE
            binding.rvObservers.visibility = View.VISIBLE
        }
        binding.ivSend.setOnClickListener {
            val comment = binding.etComment.text.toString()
            val files = commentFilesAdapter.getFiles()
            if (comment.isBlank() && files.isEmpty()) {
                binding.etComment.error = "Комментарий не может быть пустым"
                return@setOnClickListener
            }
            if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка. Проверьте подключение к сети и повторите позднее",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            viewModel.addComment(userId, comment.trim(), files)
        }
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.ivAttach.setOnClickListener {
            val openDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
            }
            startActivityForResult(openDocumentIntent, ATTACH_FILES)
        }
        binding.cbBecomeExecutor.setOnClickListener {
            if (!binding.cbBecomeExecutor.isChecked) {
                val reason = EditText(activity)
                reason.hint = "Причина"
                reason.textSize = 19F
                reason.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                MaterialAlertDialogBuilder(activity as MainActivity, R.style.alertDialog).setView(
                    reason,
                    40,
                    0,
                    40,
                    0
                )
                    .setMessage("Укажите причину, по которой Вы не сможете выполнить задачу: ")
                    .setPositiveButton("Готово") { _, _ ->
                        if (reason.text.isNullOrBlank()) {
                            reason.error = "Введите название"
                        } else {
                            viewModel.addComment(
                                userId,
                                "Не могу выполнить задачу. Причина: ${reason.text.trim()}",
                                listOf()
                            )
                        }
                    }
                    .setNegativeButton("Не указывать") { dialog, _ ->
                        dialog.cancel()
                    }.show()
            }
            viewModel.changeExecutorStatus(userId, taskId, binding.cbBecomeExecutor.isChecked)
        }
        binding.tvDelete.setOnClickListener {
            viewModel.deleteTask(taskId)
        }
        binding.ivEdit.setOnClickListener {
            findNavController().navigate(
                R.id.action_showTaskInfoFragment_to_editTaskFragment,
                bundleOf("taskId" to taskId)
            )
        }
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
                        commentFilesAdapter.addFile(UserFile(uri, name, size))
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(
                            requireContext(),
                            "Файл с таким именем уже добавлен",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun onTaskClicked(taskId: String) {
        findNavController().navigate(
            R.id.action_tasksListFragment_to_showTaskInfoFragment,
            bundleOf("taskId" to taskId)
        )
    }

    private fun bindTask(task: Task) {
        binding.etName.setText(task.title)
        if (task.deadline != null) {
            binding.tvDeadline.visibility = View.VISIBLE
            binding.tvDeadlineDate.text =
                FamilyPlanner.uiDateFormatter.format(LocalDate.ofEpochDay(task.deadline!!))
        } else {
            binding.tvDeadline.visibility = View.GONE
            binding.tvDeadlineDate.visibility = View.GONE
        }

        if (task.isContinuous) {
            val startDateTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC).withHour(task.startTime / 60)
                    .withMinute(task.startTime % 60).withZoneSameInstant(ZoneId.systemDefault())
            val finishDateTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC).withHour(task.finishTime / 60)
                    .withMinute(task.finishTime % 60).withZoneSameInstant(ZoneId.systemDefault())
            val time = String.format(
                "Время начала и окончания задачи: %02d:%02d–%02d:%02d",
                startDateTime.hour,
                startDateTime.minute,
                finishDateTime.hour,
                finishDateTime.minute
            )
            binding.tvContinuousTime.text = time
            binding.tvContinuousTime.visibility = View.VISIBLE
        } else {
            binding.tvContinuousTime.visibility = View.GONE
        }
        binding.tvRepeatType.text = when (task.repeatType) {
            RepeatType.ONCE -> "один раз"
            RepeatType.EVERY_DAY -> "каждый день"
            RepeatType.EACH_N_DAYS -> "каждые ${task.nDays} дней"
            RepeatType.DAYS_OF_WEEK -> getRepeatFromDays(task.daysOfWeek)
        }
        if (task.location != null) {
            val mapPoint = Point(task.location!!.latitude, task.location!!.longitude)
            binding.map.mapWindow.map.move(CameraPosition(mapPoint, 15f, 0f, 0f))
            binding.map.mapWindow.map.mapObjects.addPlacemark().apply {
                geometry = mapPoint
                setIcon(ImageProvider.fromResource(requireContext(), R.drawable.map_mark))
            }
            binding.map.visibility = View.VISIBLE
            binding.tvAddress.text = task.address
        } else {
            binding.map.visibility = View.GONE
            binding.tvAddress.visibility = View.GONE
        }
        binding.ivEdit.isVisible = task.createdBy.equals(userId)
        binding.tvDelete.isVisible = task.createdBy.equals(userId)
    }

    private fun getRepeatFromDays(daysOfWeek: Int): String {
        val days = listOf(
            "понедельник",
            "вторник",
            "среда",
            "четверг",
            "пятница",
            "суббота",
            "воскресенье"
        )
        val sb = StringBuilder()
        val power = 1
        for (day in days) {
            if (power and daysOfWeek > 0) {
                sb.append("${day}, ")
            }
        }
        return sb.substring(0, sb.length - 2)
    }

    private fun downloadTaskFile(path: String) {
        downloadFile("task", path, taskId)
    }

    private fun downloadCommentFile(commentId: String, path: String) {
        downloadFile("comment", path, commentId)
    }

    private fun downloadFile(prefix: String, path: String, objectId: String) {
        if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
            Toast.makeText(requireContext(), "Нет сети. Файлы недоступны", Toast.LENGTH_SHORT).show()
            return
        }
        val request = DownloadManager.Request(viewModel.downloadFile(prefix, objectId, path))
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