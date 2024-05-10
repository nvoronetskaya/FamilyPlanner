package com.familyplanner.events.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.events.data.Event
import com.familyplanner.events.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class EventsListViewModel : ViewModel() {
    private var curDate = LocalDateTime.now()
    private var collectEvents: Job? = null
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
    private var showNonVisiting = true

    fun updateEvents(newDate: LocalDateTime = curDate) {
        viewModelScope.launch(Dispatchers.IO) {
            collectEvents?.cancelAndJoin()
            curDate = newDate
            collectEvents = launch(Dispatchers.IO) {
                val start = curDate.minusDays(curDate.dayOfMonth.toLong())
                val finish = curDate.minusDays(curDate.dayOfMonth.toLong() - 1).plusMonths(1)
                if (showNonVisiting) {
                    repo.getEventsForPeriod(
                        FamilyPlanner.userId,
                        start.atZone(ZoneId.systemDefault()).toEpochSecond(),
                        finish.atZone(ZoneId.systemDefault()).toEpochSecond()
                    ).collect {
                        events.emit(it)
                    }
                } else {
                    repo.getAttendingEventsForPeriod(
                        FamilyPlanner.userId,
                        start.atZone(ZoneId.systemDefault()).toEpochSecond(),
                        finish.atZone(ZoneId.systemDefault()).toEpochSecond()
                    ).collect {
                        events.emit(it)
                    }
                }
            }
            collectEvents?.start()
        }
    }

    fun previousMonth() {
        curDate = curDate.minusMonths(1)
        updateEvents(curDate)
    }

    fun nextMonth() {
        curDate = curDate.plusMonths(1)
        updateEvents(curDate)
    }

    fun currentMonthString(): String = "${months[curDate.monthValue - 1]} ${curDate.year}"

    fun currentMonth() = curDate

    fun getEvents(): Flow<List<Event>> = events

    fun showAll() {
        showNonVisiting = true
        updateEvents()
    }

    fun hideNonVisiting() {
        showNonVisiting = false
        updateEvents()
    }
}