package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.lists.model.BudgetDto
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class AllListsBudgetViewModel : ViewModel() {
    private var familyId = ""
    private val allSpendings = MutableSharedFlow<List<BudgetDto>>(replay = 1)
    private val listRepo = GroceryListRepository()
    private var startDate = LocalDate.now()
    private var finishDate = LocalDate.now()
    private var collectBudget: Job? = null

    fun getListsForFamily(familyId: String): Flow<List<BudgetDto>> {
        startUpdates(familyId, this.startDate, this.finishDate)
        return allSpendings
    }

    private fun startUpdates(familyId: String, start: LocalDate, finish: LocalDate) {
        if (familyId == this.familyId && start == startDate && finish == finishDate) {
            return
        }
        this.familyId = familyId
        this.startDate = start
        this.finishDate = finish
        viewModelScope.launch(Dispatchers.IO) {
            collectBudget?.cancelAndJoin()
            collectBudget = launch(Dispatchers.IO) {
                listRepo.getSpendingUpdates(
                    familyId,
                    startDate.toEpochDay(),
                    finishDate.toEpochDay()
                ).collect {
                    allSpendings.emit(it)
                }
            }
            collectBudget?.start()
        }
    }

    fun getStartDate(): LocalDate = startDate

    fun getFinishDate(): LocalDate = finishDate

    fun updateStartDate(startDate: LocalDate) {
        val newFinish = if (startDate > this.finishDate) startDate else this.finishDate
        startUpdates(this.familyId, startDate, newFinish)
    }

    fun updateFinishDate(finishDate: LocalDate) {
        val newStart = if (finishDate < this.startDate) startDate else this.startDate
        startUpdates(this.familyId, newStart, finishDate)
    }

    fun getLastSpendings(): List<BudgetDto>? = allSpendings.replayCache.lastOrNull()
}