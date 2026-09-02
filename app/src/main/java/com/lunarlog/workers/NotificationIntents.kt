package com.lunarlog.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.lunarlog.MainActivity

/**
 * Notification taps go through explicit intents on [MainActivity], which hosts every `lunarlog://`
 * deep link, so the PendingIntent cannot be redirected by another app.
 */
internal object NotificationIntents {
    private const val LAUNCH_FLAGS =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    private const val PENDING_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** Opens the app at its start destination (Home). */
    fun launchApp(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply { addFlags(LAUNCH_FLAGS) }
        return PendingIntent.getActivity(context, requestCode, intent, PENDING_FLAGS)
    }

    /** Opens the app at a `lunarlog://` destination. */
    fun deepLink(context: Context, uri: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri.toUri()
            addFlags(LAUNCH_FLAGS)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PENDING_FLAGS)
    }
}
