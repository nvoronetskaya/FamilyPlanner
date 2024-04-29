package com.familyplanner.auth.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
                binding.tfName.error = "Имя не может быть пустым"
                return@setOnClickListener
            }
            binding.tfName.isErrorEnabled = false
            if (binding.etBirthday.text.isNullOrBlank()) {
                binding.tfBirthday.error = "Дата рождения не может быть пустой"
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
            AlertDialog.Builder(activity as MainActivity).setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.exit()
                    (requireActivity() as MainActivity).hideBottomNavigation()
                    findNavController().navigate(R.id.action_profileFragment_to_welcomeFragment)
                }
                .setNegativeButton("Отмена") { dialog, _ ->
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
                                    "Письмо для восстановления пароля направлено на почту",
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
            password.textSize = 19F
            val dialog =
                AlertDialog.Builder(activity as MainActivity).setTitle("Введите пароль от аккаунта")
                    .setView(password)
                    .setPositiveButton("Готово", null)
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.cancel()
                    }.show()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (password.text.isNullOrBlank()) {
                    password.error = "Введите пароль"
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val checkTask = viewModel.checkPassword(password.text.toString())
                        if (checkTask == null) {
                            Toast.makeText(
                                requireContext(),
                                "Ошибка. Проверьте данные и попробуйте позднее",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            checkTask.addOnCompleteListener {
                                requireActivity().runOnUiThread {
                                    if (it.isSuccessful) {
                                        dialog.dismiss()
                                        findNavController().navigate(
                                            R.id.action_profileFragment_to_enterEmailFragment,
                                            bundleOf("changeEmail" to true, "password" to password.text.toString())
                                        )
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Ошибка. Проверьте данные и попробуйте позднее",
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