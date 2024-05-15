package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.lists.data.BudgetDto
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ListBudgetViewModel : ViewModel() {
    private var listId = ""
    private val allSpendings = MutableSharedFlow<List<BudgetDto>>(replay = 1)
    private val listRepo = GroceryListRepository()
    private var startDate = LocalDate.now()
    private var finishDate = LocalDate.now()
    private var collectBudget: Job? = null

    fun getListSpendings(listId: String): Flow<List<BudgetDto>> {
        startUpdates(listId, startDate, finishDate)
        return allSpendings
    }

    private fun startUpdates(listId: String, start: LocalDate, finish: LocalDate) {
        if (listId == this.listId && start == startDate && finish == finishDate) {
            return
        }
        this.listId = listId
        this.startDate = start
        this.finishDate = finish
        viewModelScope.launch(Dispatchers.IO) {
            collectBudget?.cancelAndJoin()
            collectBudget = launch(Dispatchers.IO) {
                listRepo.getListSpendingUpdates(
                    listId,
                    startDate.toEpochDay(),
                    finishDate.toEpochDay()
                ).collect {
                    allSpendings.emit(it)
                }
            }
            collectBudget?.start()
        }
    }

    fun getStartDate() = startDate

    fun getFinishDate() = finishDate

    fun updateStartDate(startDate: LocalDate) {
        val newFinish = if (startDate > this.finishDate) startDate else this.finishDate
        startUpdates(this.listId, startDate, newFinish)
    }

    fun updateFinishDate(finishDate: LocalDate) {
        val newStart = if (finishDate < this.startDate) finishDate else this.startDate
        startUpdates(this.listId, newStart, finishDate)
    }

    fun addSpending(moneySpent: Double, listId: String) {
        listRepo.addSpending(FamilyPlanner.userId, listId, moneySpent)
    }
}