package com.familyplanner.auth.view

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
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
import com.familyplanner.auth.viewmodel.SignInViewModel
import com.familyplanner.databinding.FragmentSignInBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignInViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SignInViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn().collect {
                    if (it.isEmpty()) {
                        activity?.runOnUiThread {
                            findNavController().navigate(R.id.action_signInFragment_to_noFamilyFragment)
                        }
                    } else {
                        activity?.runOnUiThread {
                            binding.bEnter.isEnabled = true
                            Toast.makeText(
                                activity,
                                it,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        binding.bEnter.setOnClickListener {
            binding.bEnter.isEnabled = false
            if (binding.etEmail.text.isNullOrBlank()) {
                binding.tfEmail.error = "Введите почту"
                binding.bEnter.isEnabled = true
                return@setOnClickListener
            }
            binding.tfEmail.isErrorEnabled = false
            if (binding.etPassword.text.isNullOrBlank()) {
                binding.tfPassword.error = "Введите пароль"
                binding.bEnter.isEnabled = true
                return@setOnClickListener
            }
            binding.tfPassword.isErrorEnabled = false
            viewModel.signIn(
                binding.etEmail.text!!.trim().toString(),
                binding.etPassword.text!!.trim().toString()
            )
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = EditText(activity)
            email.hint = "Адрес почты"
            email.textSize = 17F
            email.typeface = Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
            MaterialAlertDialogBuilder(activity as MainActivity, R.style.alertDialog).setTitle("Сброс пароля").setView(email, 40, 0, 40, 0)
                .setPositiveButton("Готово") { _, _ ->
                    if (email.text.isNullOrBlank()) {
                        email.error = "Введите почту"
                    } else {
                        resetPassword(email.text.trim().toString())
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }
    }

    private fun resetPassword(email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resetPasswordResult = viewModel.resetPassword(email)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                resetPasswordResult.collect {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}