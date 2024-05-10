package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderObserverBinding
import com.familyplanner.tasks.data.ObserverDto

class ObserversListAdapter(private val userId: String) :
    RecyclerView.Adapter<ObserversListAdapter.ObserverViewHolder>() {
    private val observers = mutableListOf<ObserverDto>()

    inner class ObserverViewHolder(val binding: ViewholderObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBindViewHolder(observer: ObserverDto) {
            binding.tvName.text =
                if (userId.equals(observer.userId)) "(Вы) ${observer.userName}" else observer.userName
            binding.ivIsExecutor.isVisible = observer.isExecutor
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObserverViewHolder {
        return ObserverViewHolder(
            ViewholderObserverBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = observers.size

    override fun onBindViewHolder(holder: ObserverViewHolder, position: Int) {
        holder.onBindViewHolder(observers[position])
    }

    fun setObservers(users: List<ObserverDto>) {
        observers.clear()
        observers.addAll(users)
        notifyDataSetChanged()
    }
}