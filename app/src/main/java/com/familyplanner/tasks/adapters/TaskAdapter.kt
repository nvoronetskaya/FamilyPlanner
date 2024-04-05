package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderTaskBinding
import com.familyplanner.tasks.model.Task

class TaskAdapter(val onTaskCompleted: (String, Boolean, String) -> Unit, val userId: String) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private val tasks = mutableListOf<Task>()

    inner class TaskViewHolder(val binding: ViewholderTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(task: Task) {
            binding.tvTask.text = task.title
//            binding.cbIsDone.isChecked = TODO()
//            binding.tvDeadline = TODO()
            binding.cbIsDone.isChecked = false
            binding.tvDeadline.text = "deadline"
            binding.cbIsDone.setOnCheckedChangeListener { _, isChecked ->
                onTaskCompleted(task.id, isChecked, userId)
            }
        }
    }

    fun setTasks(tasks: List<Task>) {
        this.tasks.clear()
        this.tasks.addAll(tasks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder =
        TaskViewHolder(ViewholderTaskBinding.inflate(LayoutInflater.from(parent.context)))

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.onBind(tasks[position])
    }
}