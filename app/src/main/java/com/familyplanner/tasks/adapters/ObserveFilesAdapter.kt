package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderFileBinding

class ObserveFilesAdapter(private val onFileClick: (String) -> Unit) :
    RecyclerView.Adapter<ObserveFilesAdapter.ObserveFileViewHolder>() {
    private var paths: MutableList<String> = mutableListOf()

    inner class ObserveFileViewHolder(val binding: ViewholderFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBindViewHolder(path: String) {
            binding.tvFileName.text = path
            binding.ivRemove.visibility = View.GONE
            binding.root.setOnClickListener {
                onFileClick(path)
            }
        }
    }

    fun addPaths(newPaths: List<String>) {
        paths.clear()
        paths.addAll(newPaths)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObserveFileViewHolder {
        val binding =
            ViewholderFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ObserveFileViewHolder(binding)
    }

    override fun getItemCount(): Int = paths.size

    override fun onBindViewHolder(holder: ObserveFileViewHolder, position: Int) {
        holder.onBindViewHolder(paths[position])
    }
}