package com.familyplanner.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.databinding.FragmentGroceryListsBinding
import com.familyplanner.databinding.FragmentTasksListBinding
import com.familyplanner.tasks.viewmodel.TasksListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroceryListsFragment : Fragment() {
    private var _binding: FragmentGroceryListsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TasksListViewModel

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

        binding.rvTasks.layoutManager = LinearLayoutManager(activity)

        viewModel = ViewModelProvider(this)[TasksListViewModel::class.java]
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTasks().collect {
                    if (it.isEmpty()) {
                        activity?.runOnUiThread {
                            binding.rvTasks.visibility = View.GONE
                            binding.ivNoTasks.visibility = View.VISIBLE
                            binding.tvNoTasks.visibility = View.VISIBLE
                        }
                    } else {
                        activity?.runOnUiThread {
                            binding.rvTasks.visibility = View.VISIBLE
                            binding.ivNoTasks.visibility = View.GONE
                            binding.tvNoTasks.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}