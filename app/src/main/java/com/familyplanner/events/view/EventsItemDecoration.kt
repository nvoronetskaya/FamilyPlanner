package com.familyplanner.events.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class EventsItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildLayoutPosition(view)
        if (position > 0) {
            outRect.top = 8
        }
    }
}