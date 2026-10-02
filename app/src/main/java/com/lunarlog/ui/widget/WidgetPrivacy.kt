package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.layout.*
import androidx.glance.text.Text
import com.lunarlog.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

internal suspend fun widgetsRedacted(context: Context): Boolean = try {
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java).preferences().redactWidgets.first()
} catch (e: kotlinx.coroutines.CancellationException) { throw e }
catch (_: Exception) { true }

@Composable
internal fun RedactedWidget() {
    Box(GlanceModifier.fillMaxSize().widgetSurface().padding(16.dp).clickable(deepLinkAction(LocalContext.current, "logging")), contentAlignment = Alignment.Center) {
        Text("LunarLog • Open app to view")
    }
}
