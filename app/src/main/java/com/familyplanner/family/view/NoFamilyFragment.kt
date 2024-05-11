package com.familyplanner.family.view

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentNoFamilyBinding
import com.familyplanner.family.viewmodel.NoFamilyViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoFamilyFragment : Fragment() {
    private var _binding: FragmentNoFamilyBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoFamilyViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoFamilyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NoFamilyViewModel::class.java]
        (requireActivity() as MainActivity).showBottomNavigation()
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getUser().collect {
                        activity?.runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            if (!it.familyId.isNullOrEmpty()) {
                                binding.pbLoading.visibility = View.GONE
                                val bundle = Bundle()
                                bundle.putString("familyId", it.familyId)
                                findNavController().navigate(
                                    R.id.tasksListFragment,
                                    bundle,
                                    NavOptions.Builder().setPopUpTo(R.id.navigation, true).build()
                                )
                            } else {
                                binding.pbLoading.visibility = View.GONE
                                binding.animNothingFound.visibility = View.VISIBLE
                                binding.tvNoFamily.visibility = View.VISIBLE
                                binding.bJoin.visibility = View.VISIBLE
                                binding.bCreate.visibility = View.VISIBLE
                            }
                        }
                    }

                    launch {
                        viewModel.getErrors().collect {
                            activity?.runOnUiThread {
                                Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        binding.bCreate.setOnClickListener {
            val name = EditText(activity)
            name.hint = "Название семьи"
            name.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            name.textSize = 17f
            name.typeface =
                Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Создание семьи").setView(name, 40, 0, 40, 0)
                .setPositiveButton("Готово") { _, _ ->
                    if (name.text.isNullOrBlank()) {
                        name.error = "Введите название"
                    } else if (!(requireActivity() as MainActivity).isConnectedToInternet()) {
                        Toast.makeText(
                            requireContext(),
                            "Нет сети. Проверьте подключение и попробуйте позднее",
                            Toast.LENGTH_SHORT
                        ).show()
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
            code.textSize = 17F
            code.typeface =
                Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
            MaterialAlertDialogBuilder(
                activity as MainActivity,
                R.style.alertDialog
            ).setTitle("Присоединение к семье")
                .setView(code, 40, 0, 40, 0)
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
