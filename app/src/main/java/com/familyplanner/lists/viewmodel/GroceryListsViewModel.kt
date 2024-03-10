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
    private var userId = ""
    private val listsRepository = GroceryListRepository()
    private val groceryLists = MutableSharedFlow<List<GroceryList>>(replay = 1)

    fun setUser(userId: String) {
        this.userId = userId
        viewModelScope.launch(Dispatchers.IO) {
            listsRepository.getListsForUser(userId).collect {
                groceryLists.emit(it)
            }
        }
    }

    fun getGroceryLists(): Flow<List<GroceryList>> = groceryLists

    fun addList(name: String) {
        listsRepository.addList(name, userId)
    }

    fun changeListStatus(groceryList: GroceryList, isCompleted: Boolean) {
        listsRepository.changeListCompleted(groceryList.id, isCompleted)
    }

    fun removeList(groceryList: GroceryList) {
        listsRepository.deleteList(groceryList.id)
    }

    fun editList(list: GroceryList, newName: String) {
        listsRepository.changeListName(list.id, newName)
    }
}