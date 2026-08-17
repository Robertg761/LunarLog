package com.lunarlog.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lunarlog.workers.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Repaints the widgets the moment the calendar day changes under them.
 *
 * `ACTION_DATE_CHANGED` is what the platform sends at local midnight, and it is on the implicit
 * broadcast exception list, so unlike almost everything else it can still be declared in the manifest
 * on API 26+ and will start the app to deliver it. That matters on One UI specifically: once LunarLog
 * lands in "Sleeping apps" — which Samsung does automatically to apps the user has not opened in a
 * few days, precisely the apps whose widgets people rely on — background *polling* is suppressed,
 * while these exempt broadcasts still arrive.
 *
 * It is a belt to [WidgetRefreshWorker]'s braces rather than a replacement. The broadcast is prompt
 * but not guaranteed (some OEMs are stingy with it, and it does not arrive if the device is off at
 * midnight); the worker always eventually runs but may be deferred. Together the widgets are right
 * shortly after midnight in every case that matters. A duplicate refresh costs one redraw.
 *
 * `TIME_SET`/`TIMEZONE_CHANGED` additionally re-anchor the worker, whose 24h period was pinned to the
 * old local midnight. [com.lunarlog.workers.ReminderRescheduleReceiver] does the same for reminders;
 * this stays a separate receiver so a change to either concern cannot break the other.
 */
class WidgetDateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val appContext = context.applicationContext

        if (action != Intent.ACTION_DATE_CHANGED) {
            WidgetRefreshWorker.reschedule(appContext)
        }

        // goAsync keeps the process alive past onReceive; the widget update is a database read and a
        // RemoteViews round trip, neither of which finishes inline.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                WidgetRefresher.updateAll(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            // ACTION_TIME_CHANGED is "android.intent.action.TIME_SET" — the manual clock change.
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
