package com.familyplanner.auth.view

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.familyplanner.FamilyPlanner
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.auth.viewmodel.ProfileViewModel
import com.familyplanner.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val calendar = Calendar.getInstance()
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FamilyPlanner.userId
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getUser().collect {
                    activity?.runOnUiThread {
                        binding.etName.setText(it.name)
                        binding.etEmail.setText(it.email)
                        binding.etBirthday.setText(it.birthday)
                    }
                }
            }
        }

        binding.ivEdit.setOnClickListener {
            binding.ivEdit.visibility = View.GONE
            binding.ivDone.visibility = View.VISIBLE
            binding.etName.isEnabled = true
            binding.etBirthday.isEnabled = true
        }

        binding.etBirthday.setOnClickListener {
            val dialog = DatePickerDialog(
                activity as MainActivity,
                R.style.datePickerDialog,
                { _, year, month, day ->
                    binding.etBirthday.setText(String.format("%02d.%02d.%d", day, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.datePicker.maxDate = calendar.timeInMillis
            dialog.show()
        }

        binding.ivDone.setOnClickListener {
            if (binding.etName.text.isNullOrBlank()) {
                binding.tfName.error = resources.getString(R.string.enter_name)
                return@setOnClickListener
            }
            binding.tfName.isErrorEnabled = false
            if (binding.etBirthday.text.isNullOrBlank()) {
                binding.tfBirthday.error = resources.getString(R.string.enter_birthday)
                return@setOnClickListener
            }
            binding.tfBirthday.isErrorEnabled = false
            viewModel.updateUserInfo(
                userId,
                binding.etName.text!!.trim().toString(),
                binding.etBirthday.text.toString()
            )
            binding.etBirthday.isClickable = false
            binding.ivDone.visibility = View.GONE
            binding.ivEdit.visibility = View.VISIBLE
            binding.etName.isEnabled = false
            binding.etBirthday.isEnabled = false
        }

        binding.bExit.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.alertDialog).setTitle(resources.getString(R.string.exit))
                .setMessage(resources.getString(R.string.wanna_leave))
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    viewModel.exit()
                    (requireActivity() as MainActivity).hideBottomNavigation()
                    findNavController().navigate(R.id.action_profileFragment_to_welcomeFragment)
                }.setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.tvChangePassword.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val changePasswordResult = viewModel.changePassword()
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    changePasswordResult.collect {
                        requireActivity().runOnUiThread {
                            if (it.isEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    resources.getString(R.string.password_reset_email_sent),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    it,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        binding.tvChangeEmail.setOnClickListener {
            val password = EditText(activity)
            password.textSize = 17F
            password.typeface =
                Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
            val dialog =
                MaterialAlertDialogBuilder(activity as MainActivity, R.style.alertDialog)
                    .setTitle(resources.getString(R.string.enter_password))
                    .setView(password, 40, 0, 40, 0)
                    .setPositiveButton(resources.getString(R.string.ready), null)
                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }.show()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (password.text.isNullOrBlank()) {
                    password.error = resources.getString(R.string.enter_password)
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val checkTask = viewModel.checkPassword(password.text.toString())
                        if (checkTask == null) {
                            Toast.makeText(
                                requireContext(),
                                resources.getString(R.string.error_check_data),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            checkTask.addOnCompleteListener {
                                requireActivity().runOnUiThread {
                                    if (it.isSuccessful) {
                                        dialog.dismiss()
                                        findNavController().navigate(
                                            R.id.action_profileFragment_to_enterEmailFragment,
                                            bundleOf(
                                                "changeEmail" to true,
                                                "password" to password.text.toString()
                                            )
                                        )
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            resources.getString(R.string.error_check_data),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}