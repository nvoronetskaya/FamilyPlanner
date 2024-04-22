package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class GroceryListsViewModel : ViewModel() {
    private var userId = FamilyPlanner.userId
    private var familyId = ""
    private val listsRepository = GroceryListRepository()
    private val userRepository = UserRepository()
    private val groceryLists = MutableSharedFlow<List<GroceryList>>(replay = 1)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                familyId = userRepository.getUserByIdOnce(userId).familyId ?: ""
            }
            launch {
                listsRepository.getListsForUser(userId).collect {
                    groceryLists.emit(it)
                }
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

    fun getFamilyId() = familyId
}