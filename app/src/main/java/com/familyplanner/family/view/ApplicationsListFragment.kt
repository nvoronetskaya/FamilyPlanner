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
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.MainActivity
import com.familyplanner.databinding.FragmentApplicantsBinding
import com.familyplanner.family.adapters.ApplicationAdapter
import com.familyplanner.family.viewmodel.ApplicationListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApplicationsListFragment : Fragment() {
    private var _binding: FragmentApplicantsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ApplicationListViewModel
    private lateinit var adapter: ApplicationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentApplicantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isAdmin = requireArguments().getBoolean("isAdmin")
        val userId = requireArguments().getString("userId")!!
        viewModel = ViewModelProvider(this)[ApplicationListViewModel::class.java]
        viewModel.setUserId(userId)
        adapter = ApplicationAdapter(isAdmin, activity as MainActivity, viewModel)

        val manager = LinearLayoutManager(activity)
        binding.rvApplicants.layoutManager = manager
        binding.rvApplicants.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getErrors().collect {
                    Toast.makeText(
                        activity,
                        it,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                viewModel.getFamily().collect {
                    if (it == null) {
                        AlertDialog.Builder(activity as MainActivity)
                            .setMessage("Вы больше не являетесь участником данной семьи")
                            .setCancelable(false)
                            .setNeutralButton("Ок") { _, _ ->
                                parentFragmentManager.popBackStack()
                                parentFragmentManager.popBackStack()
                            }.create().show()
                    } else {
                        binding.tvFamily.text = it.name
                        binding.tvCode.text = it.code
                    }
                }

                viewModel.getApplicants().collect {
                    adapter.setData(it)
                }
            }
        }

        binding.itemMembers.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.ivEdit.setOnClickListener {
            val familyName = EditText(activity)
            familyName.setText(binding.tvFamily.text)
            familyName.textSize = 19F
            AlertDialog.Builder(activity as MainActivity).setTitle("Новое название семьи")
                .setView(familyName)
                .setPositiveButton("Готово") { _, _ ->
                    if (familyName.text.isNullOrBlank()) {
                        familyName.error = "Введите название"
                    } else {
                        viewModel.updateFamilyName(familyName.text.trim().toString())
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}