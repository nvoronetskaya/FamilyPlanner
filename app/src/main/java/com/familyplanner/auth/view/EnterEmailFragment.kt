package com.familyplanner.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.familyplanner.R
import com.familyplanner.auth.viewmodel.ConfirmEmailViewModel
import com.familyplanner.databinding.FragmentSignUpEmailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnterEmailFragment : Fragment() {
    private var _binding: FragmentSignUpEmailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ConfirmEmailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isChangeEmail = arguments?.getBoolean("changeEmail", false) ?: false
        val password = arguments?.getString("password") ?: ""
        viewModel = ViewModelProvider(this)[ConfirmEmailViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getLetterSent().collect { errorMessage ->
                    activity?.runOnUiThread {
                        if (_binding == null) {
                            return@runOnUiThread
                        }
                        if (errorMessage.isBlank()) {
                            val emailAddress = binding.etEmail.text?.trim().toString()
                            val bundle = Bundle()
                            bundle.putString("email", emailAddress)
                            bundle.putString("code", viewModel.getConfirmationCode())
                            bundle.putBoolean("changeEmail", isChangeEmail)
                            bundle.putString("password", password)
                            binding.pbLoading.visibility = View.GONE
                            findNavController().navigate(
                                R.id.action_enterEmailFragment_to_confirmEmailFragment,
                                bundle
                            )
                        } else {
                            activity?.runOnUiThread {
                                binding.pbLoading.visibility = View.GONE
                                Toast.makeText(
                                    activity,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                                binding.bNext.isEnabled = true
                            }
                        }
                    }
                }
            }
        }

        binding.bNext.setOnClickListener {
            it.isEnabled = false
            binding.pbLoading.visibility = View.VISIBLE
            val email = binding.etEmail.text
            if (email.isNullOrBlank()) {
                binding.pbLoading.visibility = View.GONE
                binding.tfEmail.error = resources.getString(R.string.empty_email)
                it.isEnabled = true
                return@setOnClickListener
            }
            val emailAddress = email.trim().toString()
            binding.tfEmail.error = null
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.sendConfirmationLetter(emailAddress, isChangeEmail)
            }
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