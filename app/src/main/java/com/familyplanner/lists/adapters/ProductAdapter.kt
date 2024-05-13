package com.familyplanner.lists.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderListProductBinding
import com.familyplanner.lists.data.Product

class ProductAdapter(
    val isListCreator: Boolean,
    val onEdited: (Product, String) -> Unit,
    val onStatusChanged: (Product, Boolean) -> Unit,
    val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    private val products = mutableListOf<Product>()

    inner class ProductViewHolder(val binding: ViewholderListProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(product: Product) {
            binding.etName.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    binding.etName.isEnabled = false
                    binding.ivEdit.visibility = View.VISIBLE
                    binding.ivDone.visibility = View.GONE
                }
            }
            binding.etName.setText(product.name)
            binding.ivEdit.isVisible = isListCreator
            binding.ivDelete.isVisible = isListCreator
            binding.ivEdit.setOnClickListener {
                binding.etName.isEnabled = true
                binding.etName.requestFocus()
                (binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                    binding.etName,
                    InputMethodManager.SHOW_IMPLICIT
                )
                binding.ivEdit.visibility = View.GONE
                binding.ivDone.visibility = View.VISIBLE
            }
            binding.ivDone.setOnClickListener {
                if (binding.etName.text.isNullOrBlank()) {
                    binding.etName.error = "Название не может быть пустым"
                    return@setOnClickListener
                }
                binding.etName.isEnabled = false
                binding.ivEdit.visibility = View.VISIBLE
                binding.ivDone.visibility = View.GONE
                onEdited(product, binding.etName.text.trim().toString())
            }
            binding.cbList.setOnClickListener {
                onStatusChanged(
                    product,
                    binding.cbList.isChecked
                )
            }
            binding.etName.paintFlags =
                binding.etName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.cbList.isChecked = product.isPurchased
            if (!product.isPurchased) {
                binding.etName.paintFlags = binding.etName.paintFlags xor Paint.STRIKE_THRU_TEXT_FLAG
            }
            binding.ivDelete.setOnClickListener { onDelete(product) }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductAdapter.ProductViewHolder {
        val binding =
            ViewholderListProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductAdapter.ProductViewHolder, position: Int) {
        holder.onBind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun updateData(products: List<Product>) {
        this.products.clear()
        this.products.addAll(products)
        notifyDataSetChanged()
    }
}