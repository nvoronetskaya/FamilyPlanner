package com.familyplanner.events.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.familyplanner.R
import com.familyplanner.events.data.Event
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

class EventsCalendarView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var textPaint: Paint
    private var eventTitlePaint: Paint
    private var curDayPaint: Paint
    private var eventBackgroundPaint: Paint
    private var textColor = 0
    private var boundsColor = 0
    private var currentDayColor = 0
    private var eventColor = 0
    private var dateTextSize = 0
    private var eventTextSize = 0
    private var cellWidth = 0f
    private var cellHeight = 0f
    private var rowCount = 0
    private var columnCount = 7
    private val daysOfWeek = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    private var currentDate = Calendar.getInstance()
    private var monthStart = Calendar.getInstance()
    private var monthEnd = Calendar.getInstance()
    private val events = mutableListOf<Event>()
    private val eventsByDate = Array(31) { i -> mutableListOf<Event>() }

    init {
        val attrsValues = context.obtainStyledAttributes(attrs, R.styleable.EventsCalendarView)
        textColor = attrsValues.getColor(R.styleable.EventsCalendarView_textColor, Color.BLACK)
        boundsColor = attrsValues.getColor(R.styleable.EventsCalendarView_textColor, Color.GRAY)
        currentDayColor =
            attrsValues.getColor(R.styleable.EventsCalendarView_textColor, Color.MAGENTA)
        eventColor = attrsValues.getColor(R.styleable.EventsCalendarView_textColor, Color.GREEN)
        dateTextSize =
            attrsValues.getDimensionPixelSize(R.styleable.EventsCalendarView_dateTextSize, 0)
        eventTextSize =
            attrsValues.getDimensionPixelSize(R.styleable.EventsCalendarView_eventTextSize, 0)
        attrsValues.recycle()
        rowCount = currentDate.getActualMaximum(Calendar.WEEK_OF_MONTH)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = dateTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }
        eventTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = eventColor
            textSize = eventTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }
        curDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentDayColor
            style = Paint.Style.FILL
        }
        eventBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = eventColor
            style = Paint.Style.FILL
        }
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        monthEnd.set(Calendar.DAY_OF_MONTH, monthStart.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    private fun updateEvents(newEvents: List<Event>, date: Calendar) {
        rowCount = date.getActualMaximum(Calendar.WEEK_OF_MONTH)
        this.events.clear()
        this.events.addAll(newEvents)
        groupEvents()
        invalidate()
    }

    private fun groupEvents() {
        eventsByDate.forEach { it.clear() }
        for (event in events) {
            val start = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.start),
                ZoneId.systemDefault()
            ).dayOfMonth
            val finish = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.finish),
                ZoneId.systemDefault()
            ).dayOfMonth
            for (i in start..finish) {
                eventsByDate[i].add(event)
            }
        }
    }

    private fun measureCellSize(canvas: Canvas) {
        cellWidth = 1f * canvas.width / columnCount
        cellHeight = 1f * canvas.height / rowCount
    }

    private fun drawDaysNames(canvas: Canvas) {
        for (i in 0 until columnCount) {
            val xPos = (i + 1) * cellWidth - cellWidth / 2
            canvas.drawText(daysOfWeek[i], xPos, dateTextSize * 1f, textPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        measureCellSize(canvas)
        drawDaysNames(canvas)
        var curInd = 0
        if (monthStart.get(Calendar.DAY_OF_WEEK) != 0) {
            val previousMonth = Calendar.getInstance()
            previousMonth.add(Calendar.MONTH, -1)
            val previousMonthDaysCount = previousMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val curMonthStartWeekday = monthStart.get(Calendar.DAY_OF_WEEK)
            for (i in 0 until curMonthStartWeekday) {
                val xPos = i * cellWidth
                val yPos = 0
                val xPosCenter = xPos + cellWidth / 2
                val dayNumber = previousMonthDaysCount - curMonthStartWeekday + i + 1
                canvas.drawText(
                    dayNumber.toString(),
                    xPosCenter,
                    yPos + textPaint.textSize,
                    textPaint
                )
            }
            curInd = curMonthStartWeekday
        }
        for (curDayOfMonth in 1..monthEnd.get(Calendar.DAY_OF_MONTH)) {
            val x = curInd % columnCount
            val y = curInd / columnCount
            val xPos = x * cellWidth
            val yPos = y * cellHeight
            val xPosCenter = xPos + cellWidth / 2
            canvas.drawText(
                curDayOfMonth.toString(),
                xPosCenter,
                yPos + textPaint.textSize,
                textPaint
            )
            var verticalOffset = dateTextSize * 2
            for (event in eventsByDate[curDayOfMonth - 1]) {
                if (verticalOffset - eventTextSize * 2 > cellHeight) {
                    canvas.drawText("...", xPosCenter, yPos + verticalOffset, textPaint)
                    return
                }
                canvas.drawRoundRect(
                    xPos,
                    yPos + verticalOffset,
                    xPos + cellWidth,
                    yPos + verticalOffset + eventTextSize * 2f,
                    8f,
                    8f,
                    eventBackgroundPaint
                )
                canvas.drawText(event.name, 0, cellWidth.toInt(), xPos, yPos + verticalOffset + eventTextSize / 2f, eventTitlePaint)
                verticalOffset += eventTextSize * 2
            }
        }
    }
}
