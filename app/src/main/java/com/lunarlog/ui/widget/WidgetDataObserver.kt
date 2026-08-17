package com.lunarlog.ui.widget

import android.content.Context
import com.lunarlog.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repaints every widget whenever the data behind it changes.
 *
 * Without this, logging a period in the app leaves the home screen showing yesterday's answer until
 * something else happens to trigger an update — the launcher does not poll, and Glance only rebuilds
 * a widget when asked.
 *
 * It watches Room's [androidx.room.InvalidationTracker] rather than the repositories' `Flow`s on
 * purpose. The tracker reports *which tables changed* without reading a row, so the cost of an
 * unrelated write is a set of strings; collecting `getAllCycles()` and `getAllLogs()` instead would
 * re-run both queries and map every row on every write, only to discard the results because each
 * widget re-reads what it needs in `provideGlance` anyway.
 *
 * Application-scoped is the right lifetime: no table can change while the process is dead. Writes
 * from a widget's own tap arrive through here too, though those also refresh eagerly in their
 * [androidx.glance.appwidget.action.ActionCallback] so the tick or pill responds immediately rather
 * than a debounce later.
 */
@Singleton
class WidgetDataObserver @Inject constructor(
    private val database: AppDatabase
) {

    /**
     * Starts observing. Safe to call once from `Application.onCreate`; the returned job lives as long
     * as [scope], which for the application scope means the process.
     */
    fun start(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.Default) {
            database.invalidationTracker
                .createFlow(*WATCHED_TABLES, emitInitialState = false)
                // Debounced by hand because `Flow.debounce` is still @FlowPreview. `collectLatest`
                // cancels this delay when another invalidation lands, so a multi-table transaction —
                // ending a period writes `cycles`, `log_entries` and `daily_logs` — coalesces into a
                // single pass over the widgets instead of three.
                .collectLatest {
                    delay(DEBOUNCE_MILLIS)
                    WidgetRefresher.updateAll(appContext)
                }
        }
    }

    private companion object {
        /**
         * Deliberately not `daily_logs_fts` (a shadow table of `daily_logs`, which is already here)
         * or `symptom_definitions` (no widget shows the user's custom symptom list).
         */
        val WATCHED_TABLES = arrayOf(
            "cycles",
            "daily_logs",
            "log_entries",
            "medications",
            "medication_logs"
        )

        /** Long enough to absorb a transaction's worth of table invalidations, short enough to feel immediate. */
        const val DEBOUNCE_MILLIS = 300L
    }
}
