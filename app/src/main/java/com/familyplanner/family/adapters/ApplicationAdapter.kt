package com.familyplanner.family.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.R
import com.familyplanner.common.User
import com.familyplanner.databinding.ViewholderApplicantBinding
import com.familyplanner.family.viewmodel.MembersListViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApplicationAdapter(
    val isAdmin: Boolean,
    val activity: Context,
    val viewModel: MembersListViewModel,
    val onApprove: (User) -> Unit,
    val onReject: (User) -> Unit
) :
    RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {
    private val applications = MutableList<User>(0)
    { User("", "", "", "", "") }

    inner class ApplicationViewHolder(private val binding: ViewholderApplicantBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(applicant: User) {
            binding.tvName.text = applicant.name
            binding.tvBirthday.text = applicant.birthday
            if (isAdmin) {
                binding.ivApprove.setOnClickListener {
                    onApprove(applicant)
                }

                binding.ivReject.setOnClickListener {
                    onReject(applicant)
                }
            } else {
                binding.ivApprove.visibility = View.GONE
                binding.ivReject.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val itemBinding =
            ViewholderApplicantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicationViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = applications.size

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.onBind(applications[position])
    }

    fun setData(newApplications: List<User>) {
        applications.clear()
        applications.addAll(newApplications)
        notifyDataSetChanged()
    }
}