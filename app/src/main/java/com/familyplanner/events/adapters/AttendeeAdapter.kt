package com.familyplanner.events.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.R
import com.familyplanner.databinding.ViewholderObserverBinding
import com.familyplanner.events.data.EventAttendee
import com.familyplanner.events.data.EventAttendeeStatus

class AttendeeAdapter : RecyclerView.Adapter<AttendeeAdapter.AttendeeViewHolder>() {
    private val attendees = mutableListOf<EventAttendee>()

    inner class AttendeeViewHolder(val binding: ViewholderObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(attendee: EventAttendee) {
            binding.tvName.text = attendee.userName
            val drawable = when (attendee.status) {
                EventAttendeeStatus.UNKNOWN -> R.drawable.unknown
                EventAttendeeStatus.NOT_COMING -> R.drawable.reject
                EventAttendeeStatus.COMING -> R.drawable.approve
            }
            binding.ivIsExecutor.setImageDrawable(
                AppCompatResources.getDrawable(
                    binding.root.context,
                    drawable
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendeeViewHolder {
        return AttendeeViewHolder(ViewholderObserverBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = attendees.size

    override fun onBindViewHolder(holder: AttendeeViewHolder, position: Int) {
        holder.onBind(attendees[position])
    }

    fun setData(attendees: List<EventAttendee>) {
        this.attendees.clear()
        this.attendees.addAll(attendees)
        notifyDataSetChanged()
    }
}