package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.lists.model.BudgetDto
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AllListsBudgetViewModel : ViewModel() {
    private var familyId = ""
    private val allSpendings = mutableListOf<BudgetDto>()
    private val listRepo = GroceryListRepository()
    private var startDate = LocalDate.now()
    private var finishDate = LocalDate.now()

    init {
        viewModelScope.launch(Dispatchers.IO) {

        }
    }

    suspend fun getSpendings(familyId: String): List<BudgetDto> {
        if (this.familyId != familyId) {
            this.familyId = familyId
            allSpendings.clear()
            allSpendings.addAll(listRepo.getSpending(familyId))
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
}