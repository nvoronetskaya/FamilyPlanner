package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.common.User
import com.familyplanner.databinding.ViewholderAddObserverBinding
import com.familyplanner.databinding.ViewholderObserverBinding

class AddTaskObserverAdapter(val createdBy: String) :
    RecyclerView.Adapter<AddTaskObserverAdapter.ObserverViewHolder>() {
    private val members = mutableListOf<User>()
    private var observers: BooleanArray = booleanArrayOf()
    private var executors: BooleanArray = booleanArrayOf()

    inner class ObserverViewHolder(private val binding: ViewholderAddObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int) {
            val isCreator = members[position].id.equals(createdBy)
            with(binding) {
                tvName.text = if (isCreator) "Вы" else members[position].name
                tvBirthday.text = members[position].birthday
                cbMakeObserver.isChecked = observers[position] || isCreator
                cbMakeExecutor.isChecked = executors[position]
                cbMakeExecutor.visibility = if (observers[position]) View.VISIBLE else View.GONE
                cbMakeObserver.setOnCheckedChangeListener { buttonView, isChecked ->
                    observers[position] = isChecked

                    if (isChecked) {
                        binding.cbMakeExecutor.visibility = View.VISIBLE
                    } else {
                        binding.cbMakeExecutor.isChecked = false
                        executors[position] = false
                        binding.cbMakeExecutor.visibility = View.GONE
                    }
                }
                cbMakeExecutor.setOnCheckedChangeListener { buttonView, isChecked ->
                    executors[position] = isChecked
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObserverViewHolder {
        val itemBinding =
            ViewholderAddObserverBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ObserverViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: ObserverViewHolder, position: Int) {
        holder.onBind(position)
    }

    fun setData(members: List<User>) {
        if (this.members.isEmpty()) {
            this.members.addAll(members)
            observers = BooleanArray(members.size) { false }
            executors = BooleanArray(members.size) { false }
            for (i in 0 until this.members.size) {
                if (this.members[i].id.equals(createdBy)) {
                    observers[i] = true
                }
            }
            notifyDataSetChanged()
        }
    }

    fun getMembers(): List<User> = members
    fun getObservers() = observers
    fun getExecutors() = executors
}