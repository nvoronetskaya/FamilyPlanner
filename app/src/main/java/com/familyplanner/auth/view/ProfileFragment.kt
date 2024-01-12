package com.familyplanner.auth.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = requireArguments().getString("userId")!!
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getUser().collect {
                    binding.etName.setText(it.name)
                    binding.etEmail.setText(it.email)
                    binding.etBirthday.setText(it.birthday)
                }
            }
        }

        binding.ivEdit.setOnClickListener {
            binding.ivEdit.visibility = View.GONE
            binding.ivDone.visibility = View.VISIBLE
            binding.etName.isEnabled = true

            binding.etBirthday.isClickable = true
        }

        binding.etBirthday.setOnClickListener {
            val dialog = DatePickerDialog(
                activity as MainActivity,
                { _, year, month, day ->
                    binding.etBirthday.setText(String.format("%d.%02d.%02d", year, month + 1, day))
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
                binding.etName.error = "Имя не может быть пустым"
                return@setOnClickListener
            }

            viewModel.updateUserInfo(
                userId,
                binding.etName.text!!.trim().toString(),
                binding.etBirthday.text.toString()
            )
            binding.etBirthday.isClickable = false
            binding.ivDone.visibility = View.GONE
            binding.ivEdit.visibility = View.VISIBLE
            binding.etName.isEnabled = false
        }

        binding.bExit.setOnClickListener {
            AlertDialog.Builder(activity as MainActivity).setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.exit()
                    findNavController().navigate(R.id.action_profileFragment_to_welcomeFragment)
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.tvChangePassword.setOnClickListener {
            viewModel.changePassword()
        }

        binding.tvChangeEmail.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.changeEmail().collect {
                        if (it) {
                            Toast.makeText(
                                activity,
                                "Адрес почты успешно изменён",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            AlertDialog.Builder(activity as MainActivity).setTitle("Ошибка")
                                .setMessage("Не удалось сменить адрес почты. Проверьте корректность введённого пароля и то, что этот адрес ещё не используется.")
                                .setNeutralButton("Ок") { dialog, _ ->
                                    dialog.cancel()
                                }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}