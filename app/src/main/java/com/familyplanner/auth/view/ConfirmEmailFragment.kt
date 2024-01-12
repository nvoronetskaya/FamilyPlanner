package com.familyplanner.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.familyplanner.R
import com.familyplanner.databinding.FragmentSignUpConfirmationBinding

class ConfirmEmailFragment : Fragment() {
    private var _binding: FragmentSignUpConfirmationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = requireArguments().getString("email")
        val code = requireArguments().getString("code")
        binding.tfEmail.editText?.setText(email)

        binding.bNext.setOnClickListener {
            it.isEnabled = false

            val inputCode = binding.tfCode.text
            if (inputCode.isNullOrBlank()) {
                binding.tfCode.error = "Код подтверждения не может быть пустым"
                it.isEnabled = true
                return@setOnClickListener
            }

            if (!inputCode.trim().toString().equals(code)) {
                binding.tfCode.error = "Неверный код"
                it.isEnabled = true
                return@setOnClickListener
            }

            val bundle = Bundle()
            bundle.putString("email", email)
            findNavController().navigate(R.id.action_confirmEmailFragment_to_enterInfoFragment, bundle)
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