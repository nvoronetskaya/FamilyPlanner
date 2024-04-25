package com.familyplanner.events.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.events.data.Event
import com.familyplanner.events.data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collector

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

    fun setDate(newDate: LocalDateTime = LocalDateTime.now()): String {
        viewModelScope.launch(Dispatchers.IO) {
            collectEvents?.cancelAndJoin()
            curDate = newDate
            collectEvents = launch(Dispatchers.IO) {
                val start = curDate.minusDays(curDate.dayOfMonth.toLong())
                val finish = curDate.minusDays(curDate.dayOfMonth.toLong() - 1).plusMonths(1)
                repo.getEventsForPeriod(
                    start.atZone(ZoneId.systemDefault()).toEpochSecond(),
                    finish.atZone(ZoneId.systemDefault()).toEpochSecond()
                ).collect {
                    events.emit(it)
                }
            }
            collectEvents?.start()
        }
        return "${months[newDate.monthValue - 1]} ${newDate.year}"
    }

    fun previousMonth(): String {
        curDate = curDate.minusMonths(1)
        setDate(curDate)
        return "${months[curDate.monthValue - 1]} ${curDate.year}"
    }

    fun nextMonth(): String {
        curDate = curDate.plusMonths(1)
        setDate(curDate)
        return "${months[curDate.monthValue - 1]} ${curDate.year}"
    }

    fun currentMonthString(): String = "${months[curDate.monthValue - 1]} ${curDate.year}"

    fun currentMonth() = curDate

    fun getEvents(): Flow<List<Event>> = events
}