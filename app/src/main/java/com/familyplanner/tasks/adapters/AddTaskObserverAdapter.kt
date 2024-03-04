package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.common.User
import com.familyplanner.databinding.ViewholderAddObserverBinding
import com.familyplanner.databinding.ViewholderObserverBinding

class AddTaskObserverAdapter :
    RecyclerView.Adapter<AddTaskObserverAdapter.ObserverViewHolder>() {
    private val members = mutableListOf<User>()
    private var observers: BooleanArray = booleanArrayOf()
    private var executors: BooleanArray = booleanArrayOf()

    inner class ObserverViewHolder(private val binding: ViewholderAddObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int) {
            binding.tvName.text = members[position].name
            binding.tvBirthday.text = members[position].birthday
            binding.cbMakeObserver.isChecked = observers[position]
            binding.cbMakeExecutor.isChecked = executors[position]
            binding.cbMakeExecutor.visibility = if (observers[position]) View.VISIBLE else View.GONE

            binding.cbMakeObserver.setOnCheckedChangeListener { buttonView, isChecked ->
                observers[position] = isChecked

                if (isChecked) {
                    binding.cbMakeExecutor.visibility = View.VISIBLE
                } else {
                    binding.cbMakeExecutor.isChecked = false
                    executors[position] = false
                    binding.cbMakeExecutor.visibility = View.GONE
                }
            }

            binding.cbMakeExecutor.setOnCheckedChangeListener { buttonView, isChecked ->
                executors[position] = isChecked
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
        if (members.isEmpty()) {
            this.members.addAll(members)
            notifyDataSetChanged()
            observers = BooleanArray(members.size) { false }
            executors = BooleanArray(members.size) { false }
        }
    }

    fun getMembers(): List<User> = members
    fun getObservers() = observers
    fun getExecutors() = executors
}