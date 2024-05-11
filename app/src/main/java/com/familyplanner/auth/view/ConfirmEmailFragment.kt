package com.familyplanner.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.familyplanner.R
import com.familyplanner.auth.viewmodel.ConfirmEmailViewModel
import com.familyplanner.databinding.FragmentSignUpConfirmationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConfirmEmailFragment : Fragment() {
    private var _binding: FragmentSignUpConfirmationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ConfirmEmailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isChangeEmail = arguments?.getBoolean("changeEmail", false) ?: false
        val password = arguments?.getString("password") ?: ""
        val email = requireArguments().getString("email")!!
        val code = requireArguments().getString("code")
        binding.tfEmail.editText?.setText(email)
        viewModel = ViewModelProvider(this)[ConfirmEmailViewModel::class.java]
        binding.bNext.setOnClickListener {
            it.isEnabled = false

            val inputCode = binding.tfCode.text
            if (inputCode.isNullOrBlank()) {
                binding.tfCode.error = resources.getString(R.string.empty_code)
                it.isEnabled = true
                return@setOnClickListener
            }

            if (inputCode.trim().toString() != code) {
                binding.tfCode.error = resources.getString(R.string.wrong_code)
                it.isEnabled = true
                return@setOnClickListener
            }

            val bundle = Bundle()
            bundle.putString("email", email)
            if (isChangeEmail) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val changeTask = viewModel.changeEmail(password, email)
                    changeTask?.await()
                    requireActivity().runOnUiThread {
                        if (_binding == null) {
                            return@runOnUiThread
                        }
                        if (changeTask == null || !changeTask.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                resources.getString(R.string.try_later),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            findNavController().navigate(R.id.action_confirmEmailFragment_to_profileFragment)
                        }
                    }
                }
            } else {
                findNavController().navigate(
                    R.id.action_confirmEmailFragment_to_enterInfoFragment,
                    bundle
                )
            }
        }

        binding.bBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}