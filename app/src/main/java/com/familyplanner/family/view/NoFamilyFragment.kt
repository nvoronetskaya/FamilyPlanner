package com.familyplanner.family.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.familyplanner.databinding.FragmentNoFamilyBinding
import com.familyplanner.family.viewmodel.NoFamilyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class NoFamilyFragment : Fragment() {
    private var _binding: FragmentNoFamilyBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoFamilyViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNoFamilyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NoFamilyViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getUser().collect {
                    if (it.hasFamily) {
                        parentFragmentManager.popBackStack()
                        findNavController().navigate(R.id.action_noFamilyFragment_to_tasksListFragment)
                    }
                }

                viewModel.getErrors().collect {
                    Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.bCreate.setOnClickListener {
            val name = EditText(activity)
            name.hint = "Название семьи"
            name.textSize = 19F
            AlertDialog.Builder(activity as MainActivity).setTitle("Создание семьи").setView(name)
                .setPositiveButton("Готово") { _, _ ->
                    if (name.text.isNullOrBlank()) {
                        name.error = "Введите название"
                    } else {
                        viewModel.createFamily(name.text.trim().toString())
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }

        binding.bJoin.setOnClickListener {
            val code = EditText(activity)
            code.hint = "Код присоединения"
            code.textSize = 19F
            AlertDialog.Builder(activity as MainActivity).setTitle("Присоединение к семье")
                .setView(code)
                .setPositiveButton("Готово") { _, _ ->
                    if (code.text.isNullOrBlank()) {
                        code.error = "Введите код"
                    } else {
                        viewModel.joinFamily(code.text.trim().toString())
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }

        binding.ivPerson.setOnClickListener {
            findNavController().navigate(R.id.action_noFamilyFragment_to_profileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}