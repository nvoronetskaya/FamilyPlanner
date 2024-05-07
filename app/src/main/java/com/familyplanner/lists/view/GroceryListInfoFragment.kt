package com.familyplanner.lists.view

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentGroceryItemsBinding
import com.familyplanner.lists.adapters.NonObserverAdapter
import com.familyplanner.lists.adapters.ObserversAdapter
import com.familyplanner.lists.adapters.ProductAdapter
import com.familyplanner.lists.adapters.ProductsListDecorator
import com.familyplanner.lists.model.ListObserver
import com.familyplanner.lists.model.Product
import com.familyplanner.lists.viewmodel.GroceryListInfoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroceryListInfoFragment : Fragment() {
    private var _binding: FragmentGroceryItemsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GroceryListInfoViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroceryItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listId = requireArguments().getString("listId", "")
        val isListCreator = requireArguments().getBoolean("isCreator", false)
        viewModel = ViewModelProvider(this)[GroceryListInfoViewModel::class.java]
        viewModel.setList(listId)
        val productsAdapter = ProductAdapter(
            viewModel::editProduct,
            viewModel::changeProductPurchased,
            ::onProductDelete
        )
        val observerAdapter = ObserversAdapter(isListCreator, ::onObserverDelete)
        val bottomOffset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            45f,
            requireContext().resources.displayMetrics
        )
        binding.rvProducts.addItemDecoration(ProductsListDecorator(bottomOffset.toInt()))
        binding.rvProducts.addItemDecoration(ProductsListDecorator(bottomOffset.toInt()))
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObservers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = productsAdapter
        binding.rvObservers.adapter = observerAdapter
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getListInfo().collect {
                        activity?.runOnUiThread {
                            if (it == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось найти список",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().popBackStack()
                            } else {
                                binding.tvListName.text = it.name
                            }
                        }
                    }
                }
                launch {
                    viewModel.getListProducts().collect {
                        requireActivity().runOnUiThread {
                            productsAdapter.updateData(it)
                        }
                    }
                }
                launch {
                    viewModel.getListObservers().collect {
                        requireActivity().runOnUiThread {
                            observerAdapter.updateData(it)
                        }
                    }
                }
            }
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isProducts = binding.tabs.getTabAt(0)!!.equals(tab)
                binding.rvProducts.isVisible = isProducts
                binding.rvObservers.isVisible = !isProducts
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.fabAdd.setOnClickListener {
            if (binding.tabs[0].isVisible) {
                createAddProductDialog()
            } else {
                createAddObserversDialog()
            }
        }
        binding.ivSpendings.setOnClickListener {
            findNavController().navigate(
                R.id.action_groceryListInfoFragment_to_listBudgetFragment,
                bundleOf("listId" to listId)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onProductDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext(), R.style.alertDialog).setTitle("Удаление продукта")
            .setMessage("Вы уверены, что хотите удалить продукт ${product.name}?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.deleteProduct(product)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    private fun onObserverDelete(observer: ListObserver) {
        MaterialAlertDialogBuilder(requireActivity(), R.style.alertDialog).setTitle("Удаление участника")
            .setMessage("Вы уверены, что хотите убрать участника ${observer.userName} из списка?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.deleteObserver(observer)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    private fun createAddProductDialog() {
        val name = EditText(activity)
        name.hint = "Название товара"
        name.textSize = 17F
        name.typeface = Typeface.createFromAsset(requireContext().assets, "roboto_serif.ttf")
        MaterialAlertDialogBuilder(activity as MainActivity, R.style.alertDialog).setTitle("Добавление товара")
            .setView(name, 40, 0, 40, 0)
            .setPositiveButton("Готово") { _, _ ->
                if (name.text.isNullOrBlank()) {
                    name.error = "Введите название"
                } else {
                    viewModel.addProduct(name.text.trim().toString())
                }
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    private fun createAddObserversDialog() {
        val curActivity = requireActivity()
        val builder = MaterialAlertDialogBuilder(curActivity, R.style.alertDialog)
        val view = curActivity.layoutInflater.inflate(R.layout.dialog_add_list_observers, null)
        val nonObserversList = view.findViewById<RecyclerView>(R.id.rv_observers)
        nonObserversList.layoutManager = LinearLayoutManager(requireContext())
        val nonObserversAdapter = NonObserverAdapter()
        nonObserversAdapter.updateData(viewModel.getNonObservers())

        builder.setView(view, 40, 0, 40, 0)
        builder.setPositiveButton("Готово") { dialog, _ ->
            viewModel.addObservers(nonObserversAdapter.getObservers())
            dialog.dismiss()
        }.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}