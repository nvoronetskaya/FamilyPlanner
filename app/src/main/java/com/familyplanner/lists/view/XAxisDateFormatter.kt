package com.familyplanner.lists.view

import com.familyplanner.FamilyPlanner
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDate

class XAxisDateFormatter : IndexAxisValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return LocalDate.ofEpochDay(value.toLong()).format(FamilyPlanner.uiDateFormatter)
    }
}