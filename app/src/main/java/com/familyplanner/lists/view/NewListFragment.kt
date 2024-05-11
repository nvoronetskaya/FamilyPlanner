package com.familyplanner.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyplanner.databinding.FragmentNewListBinding
import com.familyplanner.events.adapters.InvitationAdapter
import com.familyplanner.lists.viewmodel.NewListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewListFragment : Fragment() {
    private var _binding: FragmentNewListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NewListViewModel
    private lateinit var attendeesAdapter: InvitationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val familyId = requireArguments().getString("familyId")!!
        viewModel = ViewModelProvider(this)[NewListViewModel::class.java]
        attendeesAdapter = InvitationAdapter()
        binding.rvObservers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObservers.adapter = attendeesAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            val users = viewModel.getFamilyMembers(familyId)
            requireActivity().runOnUiThread {
                if (_binding == null) {
                    return@runOnUiThread
                }
                attendeesAdapter.setData(users)
            }
        }
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.ivDone.setOnClickListener {
            viewModel.createList(binding.etName.text!!.toString().trim(), attendeesAdapter.getInvitations())
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}