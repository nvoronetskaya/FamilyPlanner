package com.familyplanner.tasks.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.R
import com.familyplanner.databinding.FragmentTaskInfoBinding
import com.familyplanner.tasks.adapters.CommentsListAdapter
import com.familyplanner.tasks.adapters.ObserversListAdapter
import com.familyplanner.tasks.model.RepeatType
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.viewmodel.TaskInfoViewModel
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.Line
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class ShowTaskInfoFragment : Fragment() {
    private var _binding: FragmentTaskInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TaskInfoViewModel
    private val formatter = SimpleDateFormat("dd.MM.yyyy")
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = requireArguments().getString("taskId")!!
        viewModel = ViewModelProvider(this)[TaskInfoViewModel::class.java]
        viewModel.setTask(taskId)

        var task: Task? = null
        val commentsAdapter = CommentsListAdapter()
        val observersAdapter = ObserversListAdapter()
        userId = requireArguments().getString("userId")!!
        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentsAdapter
        binding.rvObservers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObservers.adapter = observersAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTask().collect {
                    task = it
                    bindTask(it)
                }
                viewModel.getComments().collect {
                    commentsAdapter.setComments(it)
                }
                viewModel.getObservers().collect {
                    observersAdapter.setObservers(it)
                }
                viewModel.getSubtasks().collect {

                }
            }
        }
        binding.ivAddSubtask.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("parentId", taskId)
            bundle.putString("familyId", task!!.familyId)
            bundle.putBoolean("isPrivate", task!!.isPrivate)
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
    }

    private fun bindTask(task: Task) {
        binding.etName.setText(task.title)
        if (task.hasDeadline) {
            binding.tvDeadline.visibility = View.VISIBLE
            binding.tvDeadlineDate.text = formatter.format(task.deadline)
        } else {
            binding.tvDeadline.visibility = View.GONE
            binding.tvDeadlineDate.visibility = View.GONE
        }

        if (task.isContinuous) {
            val time = "${timeToString(task.startTime)}–${task.finishTime}"
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
        if (task.hasLocation) {
            binding.map.mapWindow.map.mapObjects.addPlacemark().apply {
                geometry = Point(task.location!!.latitude, task.location!!.longitude)
                setIcon(ImageProvider.fromResource(requireContext(), R.drawable.location))
            }
            binding.map.visibility = View.VISIBLE
        } else {
            binding.map.visibility = View.GONE
        }
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

    private fun timeToString(time: Int): String = "${time / 60}:${time % 60}"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}