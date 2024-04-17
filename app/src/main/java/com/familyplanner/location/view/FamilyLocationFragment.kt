package com.familyplanner.location.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.familyplanner.databinding.FragmentLocationBinding
import com.familyplanner.location.viewmodel.FamilyLocationViewModel
import com.yandex.mapkit.MapKitFactory

class FamilyLocationFragment : Fragment() {
    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FamilyLocationViewModel

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[FamilyLocationViewModel::class.java]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}