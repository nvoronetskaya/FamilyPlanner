package com.familyplanner.lists.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderListNonObserverBinding
import com.familyplanner.lists.model.NonObserver

class NonObserverAdapter : RecyclerView.Adapter<NonObserverAdapter.NonObserverViewHolder>() {
    private val nonObservers = mutableListOf<NonObserver>()

    inner class NonObserverViewHolder(val binding: ViewholderListNonObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(nonObserver: NonObserver) {
            binding.cbListObserver.text = nonObserver.userName
            binding.cbListObserver.isChecked = nonObserver.isObserver
            binding.cbListObserver.setOnCheckedChangeListener { _, isChecked ->
                nonObserver.isObserver = isChecked
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NonObserverAdapter.NonObserverViewHolder {
        val binding =
            ViewholderListNonObserverBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return NonObserverViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NonObserverAdapter.NonObserverViewHolder, position: Int) {
        holder.onBind(nonObservers[position])
    }

    override fun getItemCount(): Int = nonObservers.size

    fun updateData(nonObservers: List<NonObserver>) {
        this.nonObservers.clear()
        this.nonObservers.addAll(nonObservers)
        notifyDataSetChanged()
    }

    fun getObservers(): List<NonObserver> = nonObservers.filter { it.isObserver }
}