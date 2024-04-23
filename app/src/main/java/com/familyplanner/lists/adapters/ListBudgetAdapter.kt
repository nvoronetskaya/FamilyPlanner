package com.familyplanner.lists.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.ViewholderBudgetBinding
import com.familyplanner.lists.model.BudgetDto

class ListBudgetAdapter : RecyclerView.Adapter<ListBudgetAdapter.BudgetViewHolder>() {
    private val budgetObjects = mutableListOf<BudgetDto>()

    inner class BudgetViewHolder(val binding: ViewholderBudgetBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(spending: BudgetDto) {
            binding.tvName.text = spending.userName
            spending.listName?.let { binding.tvListTitle.text = it }
            binding.tvTime.text = spending.addedAt.format(FamilyPlanner.dateTimeFormatter)
            binding.tvMoney.text = spending.sumSpent.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        return BudgetViewHolder(ViewholderBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = budgetObjects.size

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.onBind(budgetObjects[position])
    }

    fun setData(newBudget: List<BudgetDto>) {
        budgetObjects.clear()
        budgetObjects.addAll(newBudget)
    }
}