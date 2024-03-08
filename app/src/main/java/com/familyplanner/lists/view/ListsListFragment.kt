package com.familyplanner.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.R
import com.familyplanner.databinding.FragmentGroceryListsBinding
import com.familyplanner.lists.adapters.ListAdapter
import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.viewmodel.GroceryListsViewModel
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
    ): View? {
        _binding = FragmentGroceryListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = requireArguments().getString("userId")!!
        binding.rvLists.layoutManager = LinearLayoutManager(activity)
        val listsAdapter =
            ListAdapter(viewModel::changeListStatus, ::openList, ::onListDelete, userId)
        binding.rvLists.adapter = listsAdapter
        viewModel = ViewModelProvider(this)[GroceryListsViewModel::class.java]
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getGroceryLists().collect {
                    listsAdapter.updateData(it)
                }
            }
        }
        binding.ivAdd.setOnClickListener {

        }
    }

    private fun openList(listId: String, isCreator: Boolean) {
        val bundle = Bundle()
        bundle.putString("listId", listId)
        bundle.putBoolean("isListCreator", isCreator)
        findNavController().navigate(R.id.action_listsListFragment_to_groceryListInfoFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onListDelete(list: GroceryList) {
        AlertDialog.Builder(requireActivity()).setTitle("Удаление списка")
            .setMessage("Вы уверены, что хотите удалить список ${list.name}?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.removeList(list)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }
}