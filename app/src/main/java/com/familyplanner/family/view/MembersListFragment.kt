package com.familyplanner.family.view

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.common.data.User
import com.familyplanner.databinding.FragmentMembersBinding
import com.familyplanner.family.adapters.ApplicationAdapter
import com.familyplanner.family.adapters.MemberAdapter
import com.familyplanner.family.viewmodel.MembersListViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MembersListFragment : Fragment() {
    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MembersListViewModel
    private lateinit var adapter: MemberAdapter
    private lateinit var applicationsAdapter: ApplicationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isAdmin = requireArguments().getBoolean("isAdmin")
        val userId = FamilyPlanner.userId
        viewModel = ViewModelProvider(this)[MembersListViewModel::class.java]
        adapter = MemberAdapter(isAdmin, userId, activity as MainActivity, viewModel)

        val manager = LinearLayoutManager(activity)
        binding.rvMembers.layoutManager = manager
        binding.rvMembers.adapter = adapter
        applicationsAdapter = ApplicationAdapter(
            activity as MainActivity,
            viewModel,
            ::onApprove,
            ::onReject
        )

        val applicationsManager = LinearLayoutManager(activity)
        binding.rvApplicants.layoutManager = applicationsManager
        binding.rvApplicants.adapter = applicationsAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getErrors().collect {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                activity,
                                it,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                launch {
                    viewModel.getFamily().collect {
                        requireActivity().runOnUiThread {
                            if (it == null) {
                                MaterialAlertDialogBuilder(
                                    activity as MainActivity,
                                    R.style.alertDialog
                                ).setMessage("Вы больше не являетесь участником данной семьи")
                                    .setPositiveButton("Ок") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create().show()
                            } else {
                                if (_binding == null) {
                                    return@runOnUiThread
                                }
                                binding.tvFamily.text = it.name
                                binding.tvCode.text = it.id
                            }
                        }
                    }
                }

                launch {
                    viewModel.getMembers().collect {
                        requireActivity().runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            adapter.setData(it)
                        }
                    }
                }

                launch {
                    viewModel.getApplicants().collect {
                        requireActivity().runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            applicationsAdapter.setData(it)
                        }
                    }
                }
            }
        }
        binding.llDelete.isVisible = isAdmin
        (binding.tabs.getChildAt(0) as LinearLayout).getChildAt(1).isClickable = isAdmin
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isMembers = binding.tabs.getTabAt(0)!!.equals(tab)
                binding.rvMembers.isVisible = isMembers
                binding.rvApplicants.isVisible = !isMembers
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        binding.ivEdit.isVisible = isAdmin
        binding.ivEdit.setOnClickListener {
            val familyName = EditText(activity)
            familyName.setText(binding.tvFamily.text)
            familyName.textSize = 17F
            familyName.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(15))
            familyName.typeface =
                Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
            familyName.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Новое название семьи")
                .setView(familyName, 40, 0, 40, 0)
                .setPositiveButton("Готово") { _, _ ->
                    if (familyName.text.isNullOrBlank()) {
                        familyName.error = "Введите название"
                    } else {
                        viewModel.updateFamilyName(familyName.text.trim().toString())
                    }
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.llLeave.setOnClickListener {
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Выход из семьи")
                .setMessage("Вы уверены, что хотите покинуть семью?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.leave(userId)
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.llDelete.setOnClickListener {
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Удаление семьи")
                .setMessage("Вы уверены, что хотите удалить семью?")
                .setPositiveButton("Да") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            viewModel.deleteFamily().collect {
                                requireActivity().runOnUiThread {
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
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun onApprove(applicant: User) {
        if ((requireActivity() as MainActivity).isConnectedToInternet()) {
            viewModel.approve(applicant.id)
        } else {
            Toast.makeText(
                requireContext(),
                "Нет подключения к сети. Проверьте соединение и попробуйте позднее",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onReject(applicant: User) {
        MaterialAlertDialogBuilder(
            requireActivity(),
            R.style.alertDialog
        ).setTitle("Отклонение заявки")
            .setMessage("Вы уверены, что хотите отклонить заявку пользователя ${applicant.name}?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.reject(applicant.id)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}