package com.familyplanner.lists.view

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    ): View? {
        _binding = FragmentGroceryItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listId = """requireArguments().getString("listId", "")"""
        val familyId = """requireArguments().getString("familyId", "")"""
        val isListCreator = true
        viewModel = ViewModelProvider(this)[GroceryListInfoViewModel::class.java]
        viewModel.setList(listId, familyId)
        val productsAdapter = ProductAdapter(viewModel::changeProductPurchased, ::onProductDelete)
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
        val products = mutableListOf<Product>()
        for (i in 0..20) {
            products.add(Product("", "name", false))
        }
        productsAdapter.updateData(products)

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                            binding.tvListName.setText(it.name)
                        }
                    }
                }
                viewModel.getListProducts().collect {
                    productsAdapter.updateData(it)
                }
                viewModel.getListObservers().collect {
                    observerAdapter.updateData(it)
                }
            }
        }
        binding.tabs[0].setOnClickListener {
            binding.rvProducts.visibility = View.GONE
            binding.rvObservers.visibility = View.VISIBLE
        }
        binding.tabs[1].setOnClickListener {
            binding.rvObservers.visibility = View.GONE
            binding.rvProducts.visibility = View.VISIBLE
        }
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.fabAdd.setOnClickListener {
            if (binding.tabs[0].isVisible) {
                createAddProductDialog()
            } else {
                createAddObserversDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onProductDelete(product: Product) {
        AlertDialog.Builder(requireActivity()).setTitle("Удаление продукта")
            .setMessage("Вы уверены, что хотите удалить продукт ${product.name}?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.deleteProduct(product)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    private fun onObserverDelete(observer: ListObserver) {
        AlertDialog.Builder(requireActivity()).setTitle("Удаление участника")
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
        name.textSize = 19F
        AlertDialog.Builder(activity as MainActivity).setTitle("Добавление товара")
            .setView(name)
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
        val builder = AlertDialog.Builder(curActivity)
        val view = curActivity.layoutInflater.inflate(R.layout.dialog_add_list_observers, null)
        val nonObserversList = view.findViewById<RecyclerView>(R.id.rv_observers)
        nonObserversList.layoutManager = LinearLayoutManager(requireContext())
        val nonObserversAdapter = NonObserverAdapter()
        nonObserversAdapter.updateData(viewModel.getNonObservers())

        builder.setView(view)
        builder.setPositiveButton("Готово") { dialog, _ ->
            viewModel.addObservers(nonObserversAdapter.getObservers())
            dialog.dismiss()
        }.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}