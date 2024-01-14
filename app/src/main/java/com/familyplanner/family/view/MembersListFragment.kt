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
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentMembersBinding
import com.familyplanner.family.adapters.MemberAdapter
import com.familyplanner.family.viewmodel.MembersListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MembersListFragment : Fragment() {
    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MembersListViewModel
    private lateinit var adapter: MemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isAdmin = requireArguments().getBoolean("isAdmin")
        val userId = requireArguments().getString("userId")!!
        viewModel = ViewModelProvider(this)[MembersListViewModel::class.java]
        viewModel.setUserId(userId)
        adapter = MemberAdapter(isAdmin, activity as MainActivity, viewModel)

        val manager = LinearLayoutManager(activity)
        binding.rvMembers.layoutManager = manager
        binding.rvMembers.adapter = adapter

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
                            }.create().show()
                    } else {
                        binding.tvFamily.text = it.name
                    }
                }

                viewModel.getMembers().collect {
                    adapter.setData(it)
                }
            }
        }

        if (isAdmin) {
            binding.llDelete.visibility = View.GONE
        } else {
            binding.llDelete.visibility = View.VISIBLE
        }

        binding.itemApplications.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("isAdmin", isAdmin)
            findNavController().navigate(R.id.action_membersListFragment_to_applicationsListFragment)
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

        binding.llLeave.setOnClickListener {
            AlertDialog.Builder(activity as MainActivity).setTitle("Выход из семьи")
                .setMessage("Вы уверены, что хотите покинуть семью?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.leave(userId)
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.llDelete.setOnClickListener {
            AlertDialog.Builder(activity as MainActivity).setTitle("Удаление семьи")
                .setMessage("Вы уверены, что хотите удалить семью?")
                .setPositiveButton("Да") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            viewModel.deleteFamily().collect {
                                if (it) {
                                    findNavController().popBackStack()
                                } else {
                                    Toast.makeText(
                                        activity,
                                        "Ошибка. Проверьте подключение к сети и попробуйте позднее",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}