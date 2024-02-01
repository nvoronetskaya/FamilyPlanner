package com.familyplanner.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderFileBinding

class FileAdapter() :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    private val filePaths = mutableListOf<String>()

    inner class FileViewHolder(private val binding: ViewholderFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int) {
            binding.tvFileName.text = filePaths[position]
            binding.ivRemove.setOnClickListener {
                filePaths.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val itemBinding =
            ViewholderFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = filePaths.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.onBind(position)
    }

    fun setData(paths: List<String>) {
        filePaths.clear()
        filePaths.addAll(paths)
        notifyDataSetChanged()
    }

    fun addPath(path: String) {
        filePaths.add(path)
        notifyItemInserted(filePaths.size - 1)
    }

    fun getPaths(): List<String> = filePaths
}