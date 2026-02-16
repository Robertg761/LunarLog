package com.lunarlog.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != "android.intent.action.TIME_SET" &&
            action != "android.intent.action.TIMEZONE_CHANGED"
        ) return

        val appContext = context.applicationContext

        // Keep this lightweight; do the actual preference read + reschedule in WorkManager.
        NotificationWorkScheduler.enqueuePeriodLogReminderReschedule(appContext)
        // Also re-anchor cycle notifications to the intended daily schedule.
        NotificationWorkScheduler.rescheduleCycleNotifications(appContext)
    }
}
