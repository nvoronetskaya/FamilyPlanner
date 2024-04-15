package com.familyplanner.events.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.events.data.Event
import com.familyplanner.events.data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class EventsListViewModel : ViewModel() {
    private val curDate = LocalDateTime.now()
    private val curDateFlow = MutableSharedFlow<LocalDateTime>(replay = 1)
    private val events = MutableSharedFlow<List<Event>>(replay = 1)
    private val repo = EventRepository()
    private val months = listOf(
        "Январь",
        "Февраль",
        "Март",
        "Апрель",
        "Май",
        "Июнь",
        "Июль",
        "Август",
        "Сентябрь",
        "Октябрь",
        "Ноябрь",
        "Декабрь"
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            curDateFlow.collect {
                val start = curDate.minusDays(curDate.dayOfMonth.toLong())
                val finish = curDate.minusDays(curDate.dayOfMonth.toLong() - 1).plusMonths(1)
                repo.getEventsForPeriod(
                    start.atZone(ZoneId.systemDefault()).toEpochSecond(),
                    finish.atZone(ZoneId.systemDefault()).toEpochSecond()
                ).collect {
                    events.emit(it)
                }
            }
        }
    }

    fun previousMonth(): String {
        curDate.minusMonths(1)
        viewModelScope.launch(Dispatchers.IO) {
            curDateFlow.emit(curDate)
        }
        return "${months[curDate.monthValue]} ${curDate.year}"
    }

    fun nextMonth(): String {
        curDate.plusMonths(1)
        viewModelScope.launch(Dispatchers.IO) {
            curDateFlow.emit(curDate)
        }
        return "${months[curDate.monthValue]} ${curDate.year}"
    }

    fun currentMonthString(): String = "${months[curDate.monthValue]} ${curDate.year}"

    fun currentMonth() = curDate

    fun getEvents(): Flow<List<Event>> = events
}