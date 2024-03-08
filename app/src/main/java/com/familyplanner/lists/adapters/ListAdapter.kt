package com.familyplanner.lists.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderListProductBinding
import com.familyplanner.lists.model.GroceryList

class ListAdapter(
    val onStatusChanged: (GroceryList, Boolean) -> Unit,
    val onClick: (String, Boolean) -> Unit,
    val onDelete: (GroceryList) -> Unit,
    val userId: String
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {
    private val lists = mutableListOf<GroceryList>()

    inner class ListViewHolder(val binding: ViewholderListProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(list: GroceryList) {
            binding.ivDelete.visibility =
                if (userId.equals(list.createdBy)) View.VISIBLE else View.GONE
            binding.cbList.text = list.name
            binding.cbList.isChecked = list.isCompleted
            binding.cbList.setOnCheckedChangeListener { _, isChecked ->
                onStatusChanged(
                    list,
                    isChecked
                )
            }
            binding.root.setOnClickListener { onClick(list.id, userId.equals(list.createdBy)) }
            binding.ivDelete.setOnClickListener { onDelete(list) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.ListViewHolder {
        val binding =
            ViewholderListProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListAdapter.ListViewHolder, position: Int) {
        holder.onBind(lists[position])
    }

    override fun getItemCount(): Int = lists.size

    fun updateData(lists: List<GroceryList>) {
        this.lists.clear()
        this.lists.addAll(lists)
        notifyDataSetChanged()
    }
}