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
     *
     * Android keys a user's per-channel choices (blocked, silent, sound, lock-screen visibility)
     * by channel id, so the two channels that replace the legacy one are seeded from it on first
     * creation. A user who had switched the old channel off, on a phone someone else can see,
     * keeps that choice instead of having health notifications quietly re-enabled by an update.
     */
    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val legacy = manager.getNotificationChannel(LEGACY_SHARED)
        manager.createNotificationChannels(
            listOf(
                channel(CYCLE_PREDICTIONS, "Cycle predictions", "Upcoming period and estimated fertile days", inherit = legacy),
                channel(LOG_REMINDERS, "Logging reminders", "Daily reminders to log how you feel", inherit = legacy),
                channel(MEDICATIONS, "Medication reminders", "Reminders for scheduled medications")
            )
        )
        if (legacy != null) manager.deleteNotificationChannel(LEGACY_SHARED)
    }

    /**
     * Settings copied from [inherit] only take effect when the channel is first created; Android
     * ignores them for a channel that already exists, which is what makes this idempotent.
     */
    private fun channel(
        id: String,
        name: String,
        description: String,
        inherit: NotificationChannel? = null
    ): NotificationChannel {
        val importance = inherit?.importance
            ?.takeIf { it >= NotificationManager.IMPORTANCE_NONE }
            ?: NotificationManager.IMPORTANCE_DEFAULT
        return NotificationChannel(id, name, importance).apply {
            this.description = description
            if (inherit != null) {
                setSound(inherit.sound, inherit.audioAttributes)
                enableVibration(inherit.shouldVibrate())
                vibrationPattern = inherit.vibrationPattern
                enableLights(inherit.shouldShowLights())
                lightColor = inherit.lightColor
                lockscreenVisibility = inherit.lockscreenVisibility
                setShowBadge(inherit.canShowBadge())
            }
        }
    }
}
