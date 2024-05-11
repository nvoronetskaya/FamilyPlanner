package com.familyplanner.family.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.family.data.CompletionDto
import com.familyplanner.family.repository.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class CompletionHistoryViewModel : ViewModel() {
    private val completionHistory = MutableSharedFlow<List<CompletionDto>>(replay = 1)
    private var startDay: Long = LocalDate.now().toEpochDay() - 7
    private var familyId = ""
    private var finishDay: Long = LocalDate.now().toEpochDay()
    private var collectHistory: Job? = null
    private val familyRepo = FamilyRepository()

    fun setFamily(familyId: String): Flow<List<CompletionDto>> {
        startUpdates(familyId, startDay, finishDay)
        return completionHistory
    }

    private fun startUpdates(familyId: String, startDate: Long, finishDate: Long) {
        if (familyId == this.familyId && startDate == startDay && finishDate == finishDay) {
            return
        }
        this.familyId = familyId
        this.startDay = startDate
        this.finishDay = finishDate
        viewModelScope.launch(Dispatchers.IO) {
            collectHistory?.cancelAndJoin()
            collectHistory = launch(Dispatchers.IO) {
                familyRepo.getCompletionHistory(this@CompletionHistoryViewModel.familyId, FamilyPlanner.userId, startDay, finishDay).collect {
                    completionHistory.emit(it)
                }
            }
            collectHistory?.start()
        }
    }

    fun updateStart(date: Long) {
        val newFinish = if (date > this.finishDay) date else this.finishDay
        startUpdates(familyId, date, newFinish)
    }

    fun updateFinish(date: Long) {
        val newStart = if (date < this.startDay) date else this.startDay
        startUpdates(familyId, newStart, date)
    }

    fun getStartDay() = startDay

    fun getFinishDay() = finishDay

    fun getHistory(): Flow<List<CompletionDto>> = completionHistory

    fun lastCompletionValues(): List<CompletionDto>? = completionHistory.replayCache.lastOrNull()
}