package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderAddObserverBinding
import com.familyplanner.tasks.data.AddObserverDto

class AddTaskObserverAdapter(val createdBy: String) :
    RecyclerView.Adapter<AddTaskObserverAdapter.ObserverViewHolder>() {
    private val members = mutableListOf<AddObserverDto>()

    inner class ObserverViewHolder(private val binding: ViewholderAddObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(member: AddObserverDto) {
            val isCreator = member.userId.equals(createdBy)
            with(binding) {
                tvName.text = if (isCreator) "Вы" else members[position].userName
                tvBirthday.text = member.birthday
                cbMakeObserver.isChecked = member.isObserver || isCreator
                cbMakeExecutor.isChecked = member.isExecutor
                cbMakeExecutor.visibility = if (member.isObserver) View.VISIBLE else View.GONE
                cbMakeObserver.setOnClickListener {
                    member.isObserver = cbMakeObserver.isChecked

                    if (cbMakeObserver.isChecked) {
                        binding.cbMakeExecutor.visibility = View.VISIBLE
                    } else {
                        binding.cbMakeExecutor.isChecked = false
                        member.isExecutor = false
                        binding.cbMakeExecutor.visibility = View.GONE
                    }
                }
                cbMakeExecutor.setOnClickListener {
                    member.isExecutor = cbMakeExecutor.isChecked
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
        holder.onBind(members[position])
    }

    fun setData(members: List<AddObserverDto>) {
        if (this.members.isEmpty()) {
            this.members.addAll(members)
            for (i in 0 until this.members.size) {
                if (this.members[i].userId.equals(createdBy)) {
                    members[i].isObserver = true
                }
            }
            notifyDataSetChanged()
        }
    }

    fun getMembers(): List<AddObserverDto> = members
}