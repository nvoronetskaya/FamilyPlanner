package com.familyplanner.family.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.common.User
import com.familyplanner.databinding.ViewholderMemberBinding
import com.familyplanner.family.viewmodel.MembersListViewModel

class MemberAdapter(
    val isAdmin: Boolean,
    val activity: Context,
    val viewModel: MembersListViewModel
) :
    RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {
    private val members = MutableList<User>(0)
    { User("", "", "", true, "", "") }

    inner class MemberViewHolder(private val binding: ViewholderMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(member: User) {
            binding.tvName.text = member.name
            binding.tvBirthday.text = member.birthday
            if (isAdmin) {
                binding.ivRemove.setOnClickListener {
                    AlertDialog.Builder(activity).setTitle("Удаление участника")
                        .setMessage("Вы уверены, что хотите удалить участника ${member.name} из семьи?")
                        .setPositiveButton("Да") { _, _ ->
                            viewModel.remove(member.id)
                        }
                        .setNegativeButton("Отмена") { dialog, _ ->
                            dialog.cancel()
                        }.create().show()
                }
            } else {
                binding.ivRemove.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val itemBinding =
            ViewholderMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.onBind(members[position])
    }

    fun setData(newMembers: List<User>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }
}