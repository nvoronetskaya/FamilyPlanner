package com.familyplanner.lists.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderMemberBinding
import com.familyplanner.lists.data.ListObserver

class ObserversAdapter(val canDeleteObservers: Boolean, val onDelete: (ListObserver) -> Unit) :
    RecyclerView.Adapter<ObserversAdapter.ObserverViewHolder>() {
    private val observers = mutableListOf<ListObserver>()

    inner class ObserverViewHolder(val binding: ViewholderMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(observer: ListObserver) {
            binding.tvName.text = observer.userName
            binding.tvBirthday.visibility = View.GONE
            if (canDeleteObservers) {
                binding.ivRemove.setOnClickListener { onDelete(observer) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObserverViewHolder {
        val binding =
            ViewholderMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ObserverViewHolder(binding)
    }

    override fun getItemCount(): Int = observers.size

    override fun onBindViewHolder(holder: ObserverViewHolder, position: Int) {
        holder.onBind(observers[position])
    }

    fun updateData(newObservers: List<ListObserver>) {
        observers.clear()
        observers.addAll(newObservers)
        notifyDataSetChanged()
    }
}