package com.familyplanner.family.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.family.data.CompletionDto
import com.familyplanner.family.data.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class CompletionHistoryViewModel : ViewModel() {
    private val completionHistory = MutableSharedFlow<List<CompletionDto>>(replay = 1)
    private var startDay: Long = LocalDate.now().toEpochDay() - 7
    private var finishDay: Long = LocalDate.now().toEpochDay()
    private var collectHistory: Job? = null
    private val familyRepo = FamilyRepository()

    init {
        startUpdates()
    }

    private fun startUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            collectHistory?.cancelAndJoin()
            collectHistory = launch(Dispatchers.IO) {
                familyRepo.getCompletionHistory(startDay, finishDay).collect {
                    completionHistory.emit(it)
                }
            }
            collectHistory?.start()
        }
    }

    fun updateStart(date: Long) {
        startDay = date
        startUpdates()
    }

    fun updateFinish(date: Long) {
        finishDay = date
        startUpdates()
    }

    fun getStartDay() = startDay

    fun getFinishDay() = finishDay

    fun getHistory(): Flow<List<CompletionDto>> = completionHistory

    fun lastCompletionValues(): List<CompletionDto>? = completionHistory.replayCache.lastOrNull()
}