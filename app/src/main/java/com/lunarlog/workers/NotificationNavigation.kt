package com.lunarlog.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.lunarlog.MainActivity

internal fun notificationDestination(context: Context, destination: String): PendingIntent =
    PendingIntent.getActivity(context, destination.hashCode(),
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "lunarlog://$destination".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
