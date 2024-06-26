package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.ViewholderTaskBinding
import com.familyplanner.tasks.data.TaskWithDate
import com.familyplanner.tasks.data.RepeatType
import com.familyplanner.tasks.data.Task
import java.time.LocalDate

class TaskAdapter(
    val onTaskCompleted: (Task, Boolean, String) -> Unit,
    val userId: String,
    val onClick: (String) -> Unit,
    var day: Long
) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private val tasks = mutableListOf<TaskWithDate>()

    inner class TaskViewHolder(val binding: ViewholderTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(task: TaskWithDate) {
            binding.tvTask.text = task.task.title
            binding.cbIsDone.isChecked =
                task.task.lastCompletionDate != null && (task.task.repeatType == RepeatType.ONCE || task.task.lastCompletionDate == LocalDate.now()
                    .toEpochDay())
            binding.tvDeadline.isVisible = task.date != null
            binding.tvDeadline.text = if (task.date != null) FamilyPlanner.uiDateFormatter.format(
                LocalDate.ofEpochDay(task.date!!)
            ) else ""
            binding.cbIsDone.isClickable = task.date != null && task.date!! <= day || task.task.repeatType == RepeatType.ONCE
            binding.cbIsDone.setOnClickListener {
                onTaskCompleted(task.task, binding.cbIsDone.isChecked, userId)
            }
            binding.root.setOnClickListener { onClick(task.task.id) }
        }
    }

    fun setTasks(tasks: List<TaskWithDate>) {
        this.tasks.clear()
        this.tasks.addAll(tasks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder =
        TaskViewHolder(ViewholderTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.onBind(tasks[position])
    }
}