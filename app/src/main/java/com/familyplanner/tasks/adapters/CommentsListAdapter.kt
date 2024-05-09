package com.familyplanner.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderOtherCommentBinding
import com.familyplanner.databinding.ViewholderUserCommentBinding
import com.familyplanner.tasks.dto.CommentDto
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat

class CommentsListAdapter(private val onFileClick: (String) -> Unit, private val userId: String) :
    RecyclerView.Adapter<CommentsListAdapter.BaseCommentViewHolder>() {
    private val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm")
    private var comments = mutableListOf<CommentDto>()

    abstract inner class BaseCommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        protected val layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        protected val filesAdapter = ObserveFilesAdapter(onFileClick)
        abstract fun onBindViewHolder(comment: CommentDto)
    }

    inner class UserViewHolder(val binding: ViewholderUserCommentBinding) :
        BaseCommentViewHolder(binding.root) {
        override fun onBindViewHolder(comment: CommentDto) {
            binding.tvComment.text = comment.text
            binding.tvSentAt.text = formatter.format(comment.createdAt)
            binding.rvFiles.layoutManager = layoutManager
            binding.rvFiles.adapter = filesAdapter
            binding.tvName.text = "Вы"
            filesAdapter.addPaths(comment.fileNames)
        }
    }

    inner class OtherUserViewHolder(val binding: ViewholderOtherCommentBinding) :
        BaseCommentViewHolder(binding.root) {
        override fun onBindViewHolder(comment: CommentDto) {
            binding.tvComment.text = comment.text
            binding.tvSentAt.text = formatter.format(comment.createdAt)
            binding.rvFiles.layoutManager = layoutManager
            binding.rvFiles.adapter = filesAdapter
            binding.tvName.text = comment.userName
            filesAdapter.addPaths(comment.fileNames)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (userId.equals(comments[position].userId)) {
            return 0
        }
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseCommentViewHolder {
        return when (viewType) {
            0 -> UserViewHolder(
                ViewholderUserCommentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            1 -> OtherUserViewHolder(
                ViewholderOtherCommentBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

            else -> throw IllegalArgumentException("Неизвестный тип комментария")
        }
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: BaseCommentViewHolder, position: Int) {
        holder.onBindViewHolder(comments[position])
    }

    fun setComments(newComments: List<CommentDto>) {
        comments.clear()
        comments.addAll(newComments.sortedBy { it.createdAt })
        notifyDataSetChanged()
    }
}