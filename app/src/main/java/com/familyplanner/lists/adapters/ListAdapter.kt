package com.familyplanner.lists.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderListProductBinding
import com.familyplanner.lists.data.GroceryList

class ListAdapter(
    val onEdited: (GroceryList, String) -> Unit,
    val onStatusChanged: (GroceryList, Boolean) -> Unit,
    val onClick: (String, Boolean) -> Unit,
    val onDelete: (GroceryList) -> Unit,
    val userId: String
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {
    private val lists = mutableListOf<GroceryList>()

    inner class ListViewHolder(val binding: ViewholderListProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(list: GroceryList) {
            binding.etName.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    binding.etName.isEnabled = false
                    binding.ivEdit.visibility = View.VISIBLE
                    binding.ivDone.visibility = View.GONE
                }
            }
            binding.ivDelete.visibility =
                if (userId.equals(list.createdBy)) View.VISIBLE else View.GONE
            binding.ivEdit.visibility =
                if (userId.equals(list.createdBy)) View.VISIBLE else View.GONE
            binding.etName.setText(list.name)
            binding.ivEdit.setOnClickListener {
                binding.etName.isEnabled = true
                binding.etName.requestFocus()
                (binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                    binding.etName,
                    InputMethodManager.SHOW_IMPLICIT
                )
                binding.ivEdit.visibility = View.GONE
                binding.ivDone.visibility = View.VISIBLE
            }
            binding.ivDone.setOnClickListener {
                if (binding.etName.text.isNullOrBlank()) {
                    binding.etName.error = "Название не может быть пустым"
                    return@setOnClickListener
                }
                binding.etName.isEnabled = false
                binding.ivEdit.visibility = View.VISIBLE
                binding.ivDone.visibility = View.GONE
                onEdited(list, binding.etName.text.trim().toString())
            }
            binding.cbList.setOnCheckedChangeListener { _, isChecked ->
                binding.etName.paintFlags =
                    binding.etName.paintFlags xor Paint.STRIKE_THRU_TEXT_FLAG
                onStatusChanged(
                    list,
                    isChecked
                )
            }
            binding.cbList.isChecked = list.isCompleted
            binding.root.setOnClickListener {
                onClick(list.id, userId.equals(list.createdBy))
            }
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