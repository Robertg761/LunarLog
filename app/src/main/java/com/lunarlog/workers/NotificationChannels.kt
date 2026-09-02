package com.lunarlog.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * One channel per kind of reminder, so a user can silence cycle predictions without also losing
 * the daily logging nudge or a medication alarm. Cycle and logging reminders used to share one
 * `lunar_log_channel`, which made that impossible.
 *
 * Channel names and descriptions appear under the app's notification settings, a screen anyone
 * holding the phone can reach, so they say what a channel is for without anything personal.
 */
object NotificationChannels {
    const val CYCLE_PREDICTIONS = "lunar_log_cycle_predictions"
    const val LOG_REMINDERS = "lunar_log_log_reminders"
    const val MEDICATIONS = "lunar_log_medications"

    /** The channel cycle and logging reminders shared before the split. */
    private const val LEGACY_SHARED = "lunar_log_channel"

    /**
     * Safe to call before every notification: creating an existing channel only refreshes its
     * name and description, and deleting an absent one is a no-op.
     */
    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(
            listOf(
                channel(CYCLE_PREDICTIONS, "Cycle predictions", "Upcoming period and estimated fertile days"),
                channel(LOG_REMINDERS, "Logging reminders", "Daily reminders to log how you feel"),
                channel(MEDICATIONS, "Medication reminders", "Reminders for scheduled medications")
            )
        )
        manager.deleteNotificationChannel(LEGACY_SHARED)
    }

    private fun channel(id: String, name: String, description: String): NotificationChannel =
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            this.description = description
        }
}
