package com.familyplanner.family.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.ViewholderStatsBinding
import com.familyplanner.family.data.CompletionDto
import java.time.LocalDate

class CompletionAdapter(val onClick: (String) -> Unit) :
    RecyclerView.Adapter<CompletionAdapter.CompletionViewHolder>() {
    private val completionHistory = mutableListOf<CompletionDto>()

    inner class CompletionViewHolder(val binding: ViewholderStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(completion: CompletionDto) {
            binding.tvTaskName.text = completion.taskName
            binding.tvUserName.text = completion.userName
            binding.tvDate.text = LocalDate.ofEpochDay(completion.completionDate).format(FamilyPlanner.uiDateFormatter)
            binding.root.setOnClickListener {
                onClick(completion.taskId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletionViewHolder {
        return CompletionViewHolder(
            ViewholderStatsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = completionHistory.size

    override fun onBindViewHolder(holder: CompletionViewHolder, position: Int) {
        holder.onBind(completionHistory[position])
    }

    fun setData(completion: List<CompletionDto>) {
        completionHistory.clear()
        completionHistory.addAll(completion)
    }
}