package com.familyplanner.lists.view

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.FamilyPlanner
import com.familyplanner.R
import com.familyplanner.databinding.FragmentGroceryListsBinding
import com.familyplanner.lists.adapters.ListAdapter
import com.familyplanner.lists.data.GroceryList
import com.familyplanner.lists.viewmodel.GroceryListsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListsListFragment : Fragment() {
    private var _binding: FragmentGroceryListsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GroceryListsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroceryListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvLists.layoutManager = LinearLayoutManager(activity)
        val bottomOffset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            45f,
            requireContext().resources.displayMetrics
        )
        viewModel = ViewModelProvider(this)[GroceryListsViewModel::class.java]
        binding.rvLists.addItemDecoration(ProductsListDecorator(bottomOffset.toInt()))
        val listsAdapter = ListAdapter(
            viewModel::editList,
            viewModel::changeListStatus,
            ::openList,
            ::onListDelete,
            FamilyPlanner.userId
        )
        binding.rvLists.adapter = listsAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getGroceryLists().collect {
                    requireActivity().runOnUiThread {
                        if (_binding == null) {
                            return@runOnUiThread
                        }
                        listsAdapter.updateData(it)
                    }
                }
            }
        }
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                R.id.action_listsListFragment_to_newListFragment,
                bundleOf("familyId" to viewModel.getFamilyId())
            )
        }
        binding.ivSpendings.setOnClickListener {
            findNavController().navigate(
                R.id.action_listsListFragment_to_allListsBudgetFragment,
                bundleOf("familyId" to viewModel.getFamilyId())
            )
        }
        binding.ivEye.setOnClickListener {
            viewModel.hideCompleted()
            binding.ivEye.isVisible = false
            binding.ivEyeClosed.isVisible = true
        }
        binding.ivEyeClosed.setOnClickListener {
            viewModel.showAll()
            binding.ivEye.isVisible = true
            binding.ivEyeClosed.isVisible = false
        }
    }

    private fun openList(listId: String, isCreator: Boolean) {
        val bundle = Bundle()
        bundle.putString("listId", listId)
        bundle.putBoolean("isListCreator", isCreator)
        findNavController().navigate(
            R.id.action_listsListFragment_to_groceryListInfoFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onListDelete(list: GroceryList) {
        MaterialAlertDialogBuilder(
            requireActivity(),
            R.style.alertDialog
        ).setTitle("Удаление списка")
            .setMessage("Вы уверены, что хотите удалить список ${list.name}?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.removeList(list)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }
}