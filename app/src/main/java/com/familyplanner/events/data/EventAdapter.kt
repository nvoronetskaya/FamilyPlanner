package com.familyplanner.events.data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.ViewholderEventBinding
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EventAdapter(val onEventClicked: ((String) -> Unit)?) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {
    private val events = mutableListOf<Event>()

    inner class EventViewHolder(val binding: ViewholderEventBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(event: Event) {
            binding.tvEventName.text = event.name
            binding.tvEventStart.text = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.start),
                ZoneId.systemDefault()
            ).format(FamilyPlanner.dateTimeFormatter)
            binding.tvEventFinish.text = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.finish),
                ZoneId.systemDefault()
            ).format(FamilyPlanner.dateTimeFormatter)
            binding.root.setOnClickListener {
                onEventClicked?.invoke(event.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(ViewholderEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = events.size

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.onBind(events[position])
    }

    fun setData(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }
}