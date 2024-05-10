package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.repository.UserRepository
import com.familyplanner.lists.data.GroceryList
import com.familyplanner.lists.data.ListObserver
import com.familyplanner.lists.data.NonObserver
import com.familyplanner.lists.data.Product
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
            launch {
                listsRepository.getListById(listId).collect {
                    list.emit(it)
                }
            }
            launch {
                listsRepository.getObserversForList(listId).collect {
                    listObservers.emit(it)
                }
            }
            launch {
                listsRepository.getProductsForList(listId).collect {
                    listProducts.emit(it)
                }
            }
            launch {
                listsRepository.getNonObservers(listId, familyId).collect {
                    nonObservers.clear()
                    nonObservers.addAll(it)
                }
            }
        }
    }

    fun editProduct(product: Product, newName: String) {

    }

    fun getListInfo(): Flow<GroceryList?> = list

    fun getListObservers(): Flow<List<ListObserver>> = listObservers

    fun getListProducts(): Flow<List<Product>> = listProducts

    fun addProduct(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            listsRepository.addProduct(name, listId)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            listsRepository.deleteProduct(product.id, listId)
        }
    }

    fun changeProductPurchased(product: Product, isPurchased: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            listsRepository.changeProductPurchased(product.id, isPurchased, listId)
        }
    }

    fun addObservers(newObservers: List<NonObserver>) {
        listsRepository.addObservers(newObservers.map { it.id }, listId)
    }

    fun deleteObserver(observer: ListObserver) {
        listsRepository.deleteObserver(observer.userId, listId)
    }

    fun getNonObservers(): List<NonObserver> = nonObservers
}