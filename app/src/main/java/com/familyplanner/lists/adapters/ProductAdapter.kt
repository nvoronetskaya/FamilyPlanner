package com.familyplanner.lists.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.databinding.ViewholderListProductBinding
import com.familyplanner.lists.model.Product

class ProductAdapter(
    val onStatusChanged: (Product, Boolean) -> Unit,
    val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    private val products = mutableListOf<Product>()

    inner class ProductViewHolder(val binding: ViewholderListProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(product: Product) {
            binding.cbList.text = product.name
            binding.cbList.isChecked = product.isPurchased
            binding.cbList.setOnCheckedChangeListener { _, isChecked ->
                onStatusChanged(
                    product,
                    isChecked
                )
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