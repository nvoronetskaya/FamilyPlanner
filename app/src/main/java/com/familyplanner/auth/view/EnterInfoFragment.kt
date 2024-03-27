package com.familyplanner.auth.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.auth.viewmodel.SignUpViewModel
import com.familyplanner.databinding.FragmentSignUpInfoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class EnterInfoFragment : Fragment() {
    private var _binding: FragmentSignUpInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignUpViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SignUpViewModel::class.java]
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn().collect {
                    activity?.runOnUiThread {
                        binding.pbLoading.visibility = View.GONE
                        if (it.isBlank()) {
                            findNavController().navigate(
                                R.id.action_enterInfoFragment_to_noFamilyFragment
                            )
                        } else {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            binding.bReady.isEnabled = true
                        }
                    }
                }
            }
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

        binding.bReady.setOnClickListener {
            it.isEnabled = false
            if (binding.etName.text.isNullOrBlank()) {
                binding.tfName.error = "Введите имя"
                it.isEnabled = true
                return@setOnClickListener
            }
            binding.tfName.isErrorEnabled = false
            if (binding.etBirthday.text.isNullOrBlank()) {
                binding.tfBirthday.error = "Введите дату рождения"
                it.isEnabled = true
                return@setOnClickListener
            }
            binding.tfBirthday.isErrorEnabled = false
            if (binding.etPassword.text.isNullOrBlank()) {
                binding.tfPassword.error = "Введите пароль"
                it.isEnabled = true
                return@setOnClickListener
            }
            if (binding.etPassword.text!!.length < 6) {
                binding.tfPassword.error = "Пароль должен состоять из хотя бы 6 символов"
                it.isEnabled = true
                return@setOnClickListener
            }
            binding.tfPassword.isErrorEnabled = false
            if (!binding.etPassword.text!!.toString()
                    .equals(binding.etRepeatPassword.text.toString())
            ) {
                binding.tfRepeatPassword.error = "Пароли не совпадают"
                it.isEnabled = true
                return@setOnClickListener
            }
            binding.tfRepeatPassword.isErrorEnabled = false
            binding.pbLoading.isVisible = true

            val email = requireArguments().getString("email")!!
            val name = binding.etName.text!!.trim().toString()
            val password = binding.etPassword.text!!.trim().toString()
            viewModel.finishSignUp(
                name,
                binding.etBirthday.text!!.trim().toString(),
                email,
                password
            )
        }

        binding.bBack.setOnClickListener {
            it.isEnabled = false
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}