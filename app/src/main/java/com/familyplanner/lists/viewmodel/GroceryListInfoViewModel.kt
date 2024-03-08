package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.model.ListObserver
import com.familyplanner.lists.model.Product
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class GroceryListInfoViewModel : ViewModel() {
    private val listsRepository = GroceryListRepository()
    private val listObservers = MutableSharedFlow<List<ListObserver>>(replay = 1)
    private val list = MutableSharedFlow<GroceryList?>(replay = 1)
    private val listProducts = MutableSharedFlow<List<Product>>(replay = 1)

    fun setList(listId: String) {
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
        }
    }

    fun getListInfo(): Flow<GroceryList?> = list

    fun getListObservers(): Flow<List<ListObserver>> = listObservers

    fun getListProducts(): Flow<List<Product>> = listProducts
}