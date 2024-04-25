package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import com.familyplanner.FamilyPlanner
import com.familyplanner.lists.model.BudgetDto
import com.familyplanner.lists.repository.GroceryListRepository
import java.time.LocalDate

class ListBudgetViewModel : ViewModel() {
    private var listId = ""
    private val allSpendings = mutableListOf<BudgetDto>()
    private val listRepo = GroceryListRepository()
    private var startDate = LocalDate.now()
    private var finishDate = LocalDate.now()

    suspend fun getSpendings(listId: String): List<BudgetDto> {
        if (this.listId != listId) {
            this.listId = listId
            allSpendings.clear()
            allSpendings.addAll(listRepo.getListSpending(listId))
            startDate = allSpendings.minOfOrNull { it.addedAt }?.toLocalDate() ?: LocalDate.now()
            finishDate = allSpendings.maxOfOrNull { it.addedAt }?.toLocalDate() ?: LocalDate.now()
        }
        return getSpendingsForPeriod()
    }

    fun getStartDate() = startDate

    fun getFinishDate() = finishDate

    fun updateStartDate(startDate: LocalDate): List<BudgetDto> {
        if (startDate > this.finishDate) {
            finishDate = startDate
        }
        this.startDate = startDate
        return getSpendingsForPeriod()
    }

    fun updateFinishDate(finishDate: LocalDate): List<BudgetDto> {
        if (finishDate < this.startDate) {
            startDate = finishDate
        }
        this.finishDate = finishDate
        return getSpendingsForPeriod()
    }

    private fun getSpendingsForPeriod(): List<BudgetDto> = allSpendings.filter {
        it.addedAt.toLocalDate().toEpochDay() >= startDate.toEpochDay() && it.addedAt.toLocalDate()
            .toEpochDay() <= finishDate.toEpochDay()
    }

    fun addSpending(moneySpent: Double, listId: String) {
        listRepo.addSpending(FamilyPlanner.userId, listId, moneySpent)
    }
}