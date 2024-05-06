package com.familyplanner.events.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.FamilyPlanner
import com.familyplanner.databinding.ViewholderEventObserverBinding
import com.familyplanner.events.data.Invitation

class InvitationAdapter : RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder>() {
    private val invitations = mutableListOf<Invitation>()

    inner class InvitationViewHolder(val binding: ViewholderEventObserverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(invitation: Invitation) {
            if (FamilyPlanner.userId == invitation.userId) {
                invitation.isInvited = true
                binding.cbMakeObserver.isClickable = false
            }
            binding.tvName.text = invitation.userName
            binding.cbMakeObserver.isChecked = invitation.isInvited
            binding.tvBirthday.text = invitation.birthday
            binding.cbMakeObserver.setOnCheckedChangeListener { _, isChecked ->
                invitation.isInvited = isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        return InvitationViewHolder(
            ViewholderEventObserverBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun getItemCount(): Int = invitations.size

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.onBind(invitations[position])
    }

    fun setData(invitations: List<Invitation>) {
        this.invitations.clear()
        this.invitations.addAll(invitations)
        notifyDataSetChanged()
    }

    fun getInvitations(): List<Invitation> = invitations
}