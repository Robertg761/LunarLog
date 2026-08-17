package com.lunarlog.ui.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.glance.action.Action
// The Intent-taking overload lives in the appwidget artifact; androidx.glance.action only offers the
// ComponentName and Activity-class variants, neither of which can carry a deep-link uri.
import androidx.glance.appwidget.action.actionStartActivity
import com.lunarlog.MainActivity

/**
 * Opens the app at one of the `lunarlog://` hosts the nav graph understands — `logging`, `calendar`,
 * `analysis`, or `details/{epochDay}`.
 *
 * The component is set explicitly rather than left to implicit resolution: an unqualified ACTION_VIEW
 * from a widget can surface a disambiguation dialog, and pinning it to [MainActivity] also keeps the
 * intent working under the debug flavour's `.debug` application id, since [ComponentName] takes the
 * package from the live context rather than a hardcoded string.
 *
 * App lock, when enabled, still gates what the user sees after the activity opens — a widget tap is
 * not a bypass.
 */
internal fun deepLinkAction(context: Context, host: String): Action =
    actionStartActivity(
        Intent(Intent.ACTION_VIEW, "lunarlog://$host".toUri()).apply {
            component = ComponentName(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
