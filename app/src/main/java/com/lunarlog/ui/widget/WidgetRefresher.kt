package com.lunarlog.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

/**
 * Redraws every LunarLog widget.
 *
 * Widgets were previously only ever refreshed by their own tap handlers, so logging a period inside
 * the app left the home screen showing yesterday's answer until the host happened to rebuild it.
 * Everything that can invalidate widget content now funnels through here:
 *
 *  - [WidgetDataObserver] — any database write, while the app process is alive.
 *  - [WidgetDateChangeReceiver] — midnight rollover, and manual clock/timezone changes.
 *  - the widgets' own [androidx.glance.appwidget.action.ActionCallback]s, after they write.
 *
 * There is deliberately no `updatePeriodMillis` on any of the providers. A cycle widget's content
 * changes once a day, so polling every 30 minutes would burn battery for nothing — and on One UI it
 * would not be dependable anyway, since Samsung's "Sleeping apps" and battery-optimisation passes
 * suppress background widget updates for apps the user has not opened recently. The date-change
 * broadcast is exempt from that throttling and is what actually keeps a sleeping app's widgets
 * honest.
 */
object WidgetRefresher {

    private val widgets: List<() -> GlanceAppWidget> = listOf(
        ::LogPeriodWidget,
        ::CycleRingWidget,
        ::CycleCalendarWidget,
        ::QuickLogWidget,
        ::MedicationWidget
    )

    /**
     * Each widget is updated independently and failures are swallowed per widget: this runs from a
     * broadcast receiver and from an application-scoped collector, and one widget whose data read
     * throws must not stop the other four from redrawing.
     */
    suspend fun updateAll(context: Context) {
        widgets.forEach { factory ->
            runCatching { factory().updateAll(context) }
        }
    }
}
