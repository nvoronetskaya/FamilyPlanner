package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class GroceryListsViewModel : ViewModel() {
    private val listsRepository = GroceryListRepository()
    private val groceryLists = MutableSharedFlow<List<GroceryList>>(replay = 1)

    fun setUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            listsRepository.getListsForUser(userId).collect {
                groceryLists.emit(it)
            }
        }
    }

    fun getGroceryLists(): Flow<List<GroceryList>> = groceryLists

    fun changeListStatus(groceryList: GroceryList, isCompleted: Boolean) {

    }

    fun removeList(groceryList: GroceryList) {

    }
}