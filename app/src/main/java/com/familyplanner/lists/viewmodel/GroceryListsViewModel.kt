package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.common.repository.UserRepository
import com.familyplanner.lists.data.GroceryList
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.ZoneId

class GroceryListsViewModel : ViewModel() {
    private var userId = FamilyPlanner.userId
    private var familyId = ""
    private val listsRepository = GroceryListRepository()
    private val userRepository = UserRepository()
    private val groceryLists = MutableSharedFlow<List<GroceryList>>(replay = 1)
    private var collectLists: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                familyId = userRepository.getUserByIdOnce(userId).familyId ?: ""
            }
            launch {
                collectLists?.cancelAndJoin()
                collectLists = launch(Dispatchers.IO) {
                    listsRepository.getListsForUser(FamilyPlanner.userId).collect {
                        groceryLists.emit(it)
                    }
                }
                collectLists?.start()
            }
        }
    }

    fun getGroceryLists(): Flow<List<GroceryList>> = groceryLists

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

    fun hideCompleted() {
        viewModelScope.launch(Dispatchers.IO) {
            collectLists?.cancelAndJoin()
            collectLists = launch(Dispatchers.IO) {
                listsRepository.getNotCompletedListsForUser(FamilyPlanner.userId).collect {
                    groceryLists.emit(it)
                }
            }
            collectLists?.start()
        }
    }

    fun showAll() {
        viewModelScope.launch(Dispatchers.IO) {
            collectLists?.cancelAndJoin()
            collectLists = launch(Dispatchers.IO) {
                listsRepository.getListsForUser(FamilyPlanner.userId).collect {
                    groceryLists.emit(it)
                }
            }
            collectLists?.start()
        }
    }
}