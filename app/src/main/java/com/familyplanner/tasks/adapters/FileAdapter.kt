package com.familyplanner.tasks.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderFileBinding
import com.familyplanner.tasks.model.UserFile

class FileAdapter :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    private val files = mutableListOf<UserFile>()

    inner class FileViewHolder(private val binding: ViewholderFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(file: UserFile) {
            binding.tvFileName.text = file.name
            binding.ivRemove.setOnClickListener {
                val position = files.indexOf(file)
                files.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val itemBinding =
            ViewholderFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.onBind(files[position])
    }

    fun addFile(uri: Uri, fileName: String, size: Double) {
        files.add(UserFile(uri, fileName, size))
        notifyItemInserted(files.size - 1)
    }

    fun getFiles(): List<UserFile> = files
}