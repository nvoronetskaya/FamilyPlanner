package com.familyplanner.events.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderEventObserverBinding
import com.familyplanner.events.data.Invitation

class AttendeeAdapter : RecyclerView.Adapter<AttendeeAdapter.AttendeeViewHolder>() {
    private val attendees = mutableListOf<Invitation>()
    inner class AttendeeViewHolder(val binding:ViewholderEventObserverBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(attendee: Invitation) {
            binding.tvName.text = attendee.userName
            binding.cbMakeObserver.isChecked = attendee.isInvited
            binding.tvBirthday.text = attendee.birthday
            binding.cbMakeObserver.setOnCheckedChangeListener { buttonView, isChecked ->
                attendee.isInvited = isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendeeViewHolder {
        return AttendeeViewHolder(ViewholderEventObserverBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun getItemCount(): Int = attendees.size

    override fun onBindViewHolder(holder: AttendeeViewHolder, position: Int) {
        holder.onBind(attendees[position])
    }

    fun setData(invitations: List<Invitation>) {
        attendees.clear()
        attendees.addAll(invitations)
        notifyDataSetChanged()
    }
}