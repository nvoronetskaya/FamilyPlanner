package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.model.ListObserver
import com.familyplanner.lists.model.NonObserver
import com.familyplanner.lists.model.Product
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GroceryListInfoViewModel : ViewModel() {
    private var listId = ""
    private var familyId: String = ""
    private val listsRepository = GroceryListRepository()
    private val userRepository = UserRepository()
    private val listObservers = MutableSharedFlow<List<ListObserver>>(replay = 1)
    private val list = MutableSharedFlow<GroceryList?>(replay = 1)
    private val listProducts = MutableStateFlow(listOf<Product>())
    private val nonObservers = mutableListOf<NonObserver>()

    init {
        viewModelScope.launch {
            userRepository.getUserById(FamilyPlanner.userId).collect {
                familyId = it.familyId!!
            }
        }
    }
    fun setList(listId: String) {
        if (listId == this.listId) {
            return
        }
        this.listId = listId
        viewModelScope.launch(Dispatchers.IO) {
            listsRepository.getListById(listId).collect {
                list.emit(it)
            }
            listsRepository.getObserversForList(listId).collect {
                listObservers.emit(it)
            }
            listsRepository.getProductsForList(listId).collect {
                listProducts.emit(it)
            }
            listsRepository.getNonObservers(listId, familyId).collect {
                nonObservers.clear()
                nonObservers.addAll(it)
            }
        }
    }

    fun editProduct(product: Product, newName: String) {

    }

    fun getListInfo(): Flow<GroceryList?> = list

    fun getListObservers(): Flow<List<ListObserver>> = listObservers

    fun getListProducts(): Flow<List<Product>> = listProducts

    fun addProduct(name: String) {
        listsRepository.addProduct(name, listId)
        listsRepository.changeListCompleted(listId, false)
    }

    fun deleteProduct(product: Product) {
        listsRepository.deleteProduct(product.id)
    }

    fun changeProductPurchased(product: Product, isPurchased: Boolean) {
        listsRepository.changeProductPurchased(product.id, isPurchased)
        listsRepository.changeListCompleted(listId, listProducts.value.all { it.isPurchased })
    }

    fun addObservers(newObservers: List<NonObserver>) {
        listsRepository.addObservers(newObservers.map { it.id }, listId)
    }

    fun deleteObserver(observer: ListObserver) {
        listsRepository.deleteObserver(observer.userId, listId)
        listsRepository.changeListCompleted(listId, listProducts.value.all { it.isPurchased })
    }

    fun getNonObservers(): List<NonObserver> = nonObservers
}