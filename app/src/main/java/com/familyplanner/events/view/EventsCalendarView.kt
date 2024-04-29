package com.familyplanner.events.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyplanner.R
import com.familyplanner.events.data.Event
import com.familyplanner.events.data.EventAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

class EventsCalendarView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var textPaint: Paint
    private var neighbourMonthPaint: Paint
    private var eventTitlePaint: TextPaint
    private var curDayPaint: Paint
    private var eventBackgroundPaint: Paint
    private var boundsPaint: Paint
    private var textColor = 0
    private var boundsColor = 0
    private var currentDayColor = 0
    private var eventTextColor = 0
    private var eventFillColor: Int
    private var dateTextSize = 0
    private var eventTextSize = 0
    private var boundsWidth = 0
    private var eventsGap = 0
    private var cellWidth = 0f
    private var cellHeight = 0f
    private var rowCount = 0
    private var columnCount = 7
    private val daysOfWeek = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    private var currentDate = Calendar.getInstance()
    private var monthStart = Calendar.getInstance()
    private var monthEnd = Calendar.getInstance()
    private val events = mutableListOf<Event>()
    private val eventsByDate = Array(31) { _ -> mutableListOf<Event>() }
    private var onEventClicked: ((String) -> Unit)? = null
    private var marginItemDecoration = EventsItemDecoration()

    init {
        val attrsValues = context.obtainStyledAttributes(attrs, R.styleable.EventsCalendarView)
        textColor = attrsValues.getColor(R.styleable.EventsCalendarView_textColor, Color.BLACK)
        boundsColor = attrsValues.getColor(R.styleable.EventsCalendarView_boundsColor, Color.GRAY)
        currentDayColor =
            attrsValues.getColor(R.styleable.EventsCalendarView_currentDayColor, Color.MAGENTA)
        eventTextColor =
            attrsValues.getColor(R.styleable.EventsCalendarView_eventTextColor, Color.BLACK)
        eventFillColor =
            attrsValues.getColor(R.styleable.EventsCalendarView_eventFillColor, Color.GRAY)
        dateTextSize =
            attrsValues.getDimensionPixelSize(R.styleable.EventsCalendarView_dateTextSize, 24)
        eventTextSize =
            attrsValues.getDimensionPixelSize(R.styleable.EventsCalendarView_eventTextSize, 24)
        boundsWidth =
            attrsValues.getDimensionPixelSize(R.styleable.EventsCalendarView_boundsWidth, 1)
        eventsGap =
            attrsValues.getDimensionPixelSize(R.styleable.EventsCalendarView_eventsGap, 1)
        attrsValues.recycle()
        rowCount = currentDate.getActualMaximum(Calendar.WEEK_OF_MONTH)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = dateTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }
        neighbourMonthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = dateTextSize.toFloat()
            textAlign = Paint.Align.CENTER
            alpha = (255 * 0.3).toInt()
        }
        eventTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = eventTextColor
            textSize = eventTextSize.toFloat()
            textAlign = Paint.Align.LEFT
        }
        curDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentDayColor
            style = Paint.Style.FILL
        }
        eventBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = eventFillColor
            style = Paint.Style.FILL
        }
        boundsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = boundsColor
        }
        monthStart.add(Calendar.DAY_OF_MONTH, -monthStart.get(Calendar.DAY_OF_MONTH) + 1)
        monthEnd.add(
            Calendar.DAY_OF_MONTH,
            monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH) - monthEnd.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun setOnEventChosen(onEventClicked: (String) -> Unit) {
        this.onEventClicked = onEventClicked
    }

    fun updateEvents(newEvents: List<Event>, date: LocalDateTime) {
        currentDate = Calendar.Builder().set(Calendar.YEAR, date.year)
            .set(Calendar.MONTH, date.monthValue - 1)
            .set(Calendar.DAY_OF_MONTH, date.dayOfMonth).build()
        monthStart = Calendar.Builder().set(Calendar.YEAR, date.year)
            .set(Calendar.MONTH, date.monthValue - 1)
            .set(Calendar.DAY_OF_MONTH, date.dayOfMonth).build()
        monthEnd = Calendar.Builder().set(Calendar.YEAR, date.year)
            .set(Calendar.MONTH, date.monthValue - 1)
            .set(Calendar.DAY_OF_MONTH, date.dayOfMonth).build()
        monthStart.add(Calendar.DAY_OF_MONTH, -monthStart.get(Calendar.DAY_OF_MONTH) + 1)
        monthEnd.add(
            Calendar.DAY_OF_MONTH,
            monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH) - monthEnd.get(Calendar.DAY_OF_MONTH)
        )
        rowCount = if (currentDate.get(Calendar.DAY_OF_WEEK) != 2) {
            (currentDate.getActualMaximum(Calendar.DAY_OF_MONTH) - 1 + (currentDate.get(Calendar.DAY_OF_WEEK) + 5) % 7) / 7 + 1
        } else {
            (currentDate.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7
        }
        this.events.clear()
        this.events.addAll(newEvents)
        groupEvents()
        invalidate()
    }

    private fun groupEvents() {
        eventsByDate.forEach { it.clear() }
        for (event in events) {
            val startDate = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.start),
                ZoneId.systemDefault()
            )
            val finishDate = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.finish),
                ZoneId.systemDefault()
            )
            if (startDate.month.value > currentDate.get(Calendar.MONTH) + 1) {
                continue
            }
            if (finishDate.month.value < currentDate.get(Calendar.MONTH) + 1) {
                continue
            }
            val start =
                if (startDate.month.value < currentDate.get(Calendar.MONTH) + 1) 0 else startDate.dayOfMonth
            val finish =
                if (finishDate.month.value > currentDate.get(Calendar.MONTH) + 1) {
                    monthEnd.get(
                        Calendar.DAY_OF_MONTH
                    )
                } else {
                    finishDate.dayOfMonth
                }
            for (i in start - 1..finish - 1) {
                eventsByDate[i].add(event)
            }
        }
    }

    private fun measureCellSize(canvas: Canvas) {
        cellWidth = 1f * canvas.width / columnCount
        cellHeight = (canvas.height - dateTextSize * 1.5f) / rowCount
    }

    private fun drawDaysNames(canvas: Canvas) {
        for (i in 0 until columnCount) {
            val xPos = (i + 1) * cellWidth - cellWidth / 2
            canvas.drawText(daysOfWeek[i], xPos, dateTextSize * 1f, textPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 0 until columnCount) {
            val x = i * cellWidth
            canvas.drawLine(x, 0f, x + boundsWidth, canvas.height.toFloat(), boundsPaint)
        }
        canvas.drawLine(0f, 0f, canvas.width.toFloat(), boundsWidth.toFloat(), boundsPaint)
        for (i in 0 until rowCount) {
            val y = i * cellHeight + dateTextSize * 1.5f
            canvas.drawLine(0f, y, canvas.width.toFloat(), y + boundsWidth, boundsPaint)
        }
        canvas.drawLine(
            0f,
            canvas.height.toFloat() - boundsWidth,
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            boundsPaint
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        measureCellSize(canvas)
        drawDaysNames(canvas)
        drawGrid(canvas)
        var curInd = 0
        val yOffset = dateTextSize * 1.5f
        if ((monthStart.get(Calendar.DAY_OF_WEEK) + 5) % 7 != 0) {
            val previousMonth = currentDate
            previousMonth.add(Calendar.MONTH, -1)
            val previousMonthDaysCount = previousMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val curMonthStartWeekday = (monthStart.get(Calendar.DAY_OF_WEEK) + 5) % 7
            for (i in 0 until curMonthStartWeekday) {
                val xPos = i * cellWidth
                val yPos = 0
                val xPosCenter = xPos + cellWidth / 2
                val dayNumber = previousMonthDaysCount - curMonthStartWeekday + i + 1
                canvas.drawText(
                    dayNumber.toString(),
                    xPosCenter,
                    yPos + textPaint.textSize + yOffset,
                    neighbourMonthPaint
                )
            }
            curInd = curMonthStartWeekday
        }
        for (curDayOfMonth in 1..monthEnd.get(Calendar.DAY_OF_MONTH)) {
            val x = curInd % columnCount
            val y = curInd / columnCount
            val xPos = x * cellWidth
            val yPos = y * cellHeight + yOffset
            val xPosCenter = xPos + cellWidth / 2
            canvas.drawText(
                curDayOfMonth.toString(),
                xPosCenter,
                yPos + textPaint.textSize,
                textPaint
            )
            var verticalOffset = dateTextSize * 2f
            val totalEvents = eventsByDate[curDayOfMonth - 1].size
            for (i in 0 until totalEvents) {
                val event = eventsByDate[curDayOfMonth - 1][i]
                canvas.drawRoundRect(
                    xPos + 2 * boundsWidth,
                    yPos + verticalOffset,
                    xPos + cellWidth - boundsWidth,
                    yPos + verticalOffset + eventTextSize * 1.5f,
                    8f,
                    8f,
                    eventBackgroundPaint
                )
                if (verticalOffset + (eventTextSize * 1.5f + eventsGap) * 2 > cellHeight) {
                    canvas.drawText(
                        "ещё ${totalEvents - i}",
                        xPos + boundsWidth * 4,
                        yPos + verticalOffset + eventTextSize * 1.5f - eventTextSize / 2f,
                        eventTitlePaint
                    )
                    break
                }
                val text = TextUtils.ellipsize(
                    event.name,
                    eventTitlePaint,
                    cellWidth - 6 * boundsWidth,
                    TextUtils.TruncateAt.END
                )
                canvas.drawText(
                    text,
                    0,
                    text.length,
                    xPos + boundsWidth * 4,
                    yPos + verticalOffset + eventTextSize * 1.5f - eventTextSize / 2f,
                    eventTitlePaint
                )
                verticalOffset += eventTextSize * 1.5f + eventsGap
            }
            ++curInd
        }
        var i = 1
        while (curInd < rowCount * 7) {
            val x = curInd % columnCount
            val y = curInd / columnCount
            val xPos = x * cellWidth
            val yPos = y * cellHeight + yOffset
            val xPosCenter = xPos + cellWidth / 2
            canvas.drawText(
                i.toString(),
                xPosCenter,
                yPos + textPaint.textSize,
                neighbourMonthPaint
            )
            ++i
            ++curInd
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            val column = (event.x / cellWidth).toInt()
            val row = ((event.y - dateTextSize * 1.5f) / cellHeight).toInt()
            val date = row * 7 + column - (monthStart.get(Calendar.DAY_OF_WEEK) + 5) % 7
            showEventsForDay(date)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun showEventsForDay(date: Int) {
        if (date > eventsByDate.size || eventsByDate[date].size == 0) {
            return
        }
        val bottomSheet = BottomSheetDialog(context)
        onEventClicked.also { bottomSheet.dismiss() }
        bottomSheet.setContentView(R.layout.bottomsheet_events_list)
        bottomSheet.behavior.isDraggable = true
        val eventsRecycler = bottomSheet.findViewById<RecyclerView>(R.id.events_list)
        val eventsAdapter = EventAdapter {
            onEventClicked?.invoke(it)
            bottomSheet.dismiss()
        }
        eventsRecycler?.layoutManager = LinearLayoutManager(context)
        eventsRecycler?.adapter = eventsAdapter
        eventsAdapter.setData(eventsByDate[date])
        eventsRecycler?.addItemDecoration(marginItemDecoration)
        bottomSheet.show()
    }
}
