package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.common.User
import com.familyplanner.databinding.ViewholderObserverBinding
import com.familyplanner.tasks.dto.ObserverDto

class ObserversListAdapter(private val userId: String) :
    RecyclerView.Adapter<ObserversListAdapter.ObserverViewHolder>() {
    private val observers = mutableListOf<ObserverDto>()

    inner class ObserverViewHolder(val binding: ViewholderObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBindViewHolder(observer: ObserverDto) {
            binding.tvName.text =
                if (userId.equals(observer.userId)) "(Вы) ${observer.userName}" else observer.userName
            binding.ivIsExecutor.visibility = if (observer.isExecutor) View.VISIBLE else View.GONE
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
    }
}