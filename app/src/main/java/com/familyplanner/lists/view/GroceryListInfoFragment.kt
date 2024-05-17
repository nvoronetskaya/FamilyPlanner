package com.familyplanner.lists.view

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
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
import com.familyplanner.common.view.MainActivity
import com.familyplanner.R
import com.familyplanner.databinding.FragmentGroceryItemsBinding
import com.familyplanner.lists.adapters.NonObserverAdapter
import com.familyplanner.lists.adapters.ObserversAdapter
import com.familyplanner.lists.adapters.ProductAdapter
import com.familyplanner.lists.data.ListObserver
import com.familyplanner.lists.data.Product
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
        val isListCreator = requireArguments().getBoolean("isListCreator", false)
        viewModel = ViewModelProvider(this)[GroceryListInfoViewModel::class.java]
        viewModel.setList(listId)
        val productsAdapter = ProductAdapter(isListCreator,
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
        binding.fabAdd.isVisible = isListCreator
        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getListInfo().collect {
                        activity?.runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
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
                            if (_binding == null) {
                                return@runOnUiThread
                            }
                            productsAdapter.updateData(it)
                        }
                    }
                }
                launch {
                    viewModel.getListObservers().collect {
                        requireActivity().runOnUiThread {
                            if (_binding == null) {
                                return@runOnUiThread
                            }
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
            if (binding.rvProducts.isVisible) {
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
        name.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
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
        lifecycleScope.launch(Dispatchers.IO) {
            val nonObservers = viewModel.getNonObservers()
            val curActivity = requireActivity()
            curActivity.runOnUiThread {
                if (_binding == null) {
                    return@runOnUiThread
                }
                val builder = MaterialAlertDialogBuilder(curActivity, R.style.alertDialog).setTitle("Добавление членов семьи")
                val view = curActivity.layoutInflater.inflate(R.layout.dialog_add_list_observers, null)
                val nonObserversList = view.findViewById<RecyclerView>(R.id.rv_observers)
                nonObserversList.layoutManager = LinearLayoutManager(requireContext())
                val nonObserversAdapter = NonObserverAdapter()
                nonObserversList.adapter = nonObserversAdapter
                if (nonObservers.isEmpty()) {
                    Toast.makeText(requireContext(), "Список доступен всем членам семьи", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                nonObserversAdapter.updateData(nonObservers)

                builder.setView(view, 40, 0, 40, 0)
                builder.setPositiveButton("Готово") { dialog, _ ->
                    viewModel.addObservers(nonObserversAdapter.getObservers())
                    dialog.dismiss()
                }.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                builder.create().show()
            }
        }
    }
}