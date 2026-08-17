package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Today's flow level and water count, as the widget needs them. */
private data class QuickLogState(
    /** 0..4, matching `DailyLog.flowLevel`. 0 means nothing logged. */
    val flowLevel: Int,
    val waterCups: Int
)

/**
 * One-tap logging for the two daily fields that have an unambiguous value: flow intensity and water.
 *
 * Everything else the app tracks — symptoms, mood, notes — needs a choice made from a list, so there
 * is no honest one-tap default for it; the "More" button hands those off to the log screen rather
 * than guessing.
 *
 * The two fields are written differently on purpose, because the daily aggregate treats them
 * differently. Water is summed across entries, so a tap appends a cup. Flow is aggregated as a max,
 * so a tap *replaces* the day's flow via [com.lunarlog.data.DailyLogRepository.replaceEntriesOfType]
 * — otherwise mistapping Heavy would be uncorrectable from the widget. Tapping the level that is
 * already set clears it.
 */
class QuickLogWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val today = LocalDate.now()
        val state = withContext(Dispatchers.IO) {
            runCatching {
                val log = entryPoint.dailyLogRepository()
                    .getLogsForRangeSync(today, today)
                    .firstOrNull()
                QuickLogState(
                    flowLevel = log?.flowLevel ?: 0,
                    waterCups = log?.waterIntake ?: 0
                )
            }.getOrDefault(QuickLogState(flowLevel = 0, waterCups = 0))
        }

        provideContent { QuickLogContent(state) }
    }

    @Composable
    private fun QuickLogContent(state: QuickLogState) {
        val isTall = LocalSize.current.height >= TALL.height

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .widgetSurface()
                .padding(horizontal = 8.dp, vertical = if (isTall) 8.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTall) {
                Text(
                    text = if (state.flowLevel == 0) "Log today's flow" else flowLabel(state.flowLevel),
                    maxLines = 1,
                    style = TextStyle(
                        color = WidgetColors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(GlanceModifier.height(6.dp))
            }

            FlowRow(state.flowLevel)

            if (isTall) {
                Spacer(GlanceModifier.height(6.dp))
                WaterRow(state.waterCups)
            }
        }
    }

    @Composable
    private fun FlowRow(selectedLevel: Int) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            FLOW_LEVELS.forEachIndexed { index, level ->
                if (index > 0) Spacer(GlanceModifier.width(4.dp))
                Pill(
                    label = flowLabel(level),
                    description = if (level == selectedLevel) {
                        "${flowLabel(level)} flow logged for today, tap to clear"
                    } else {
                        "Log ${flowLabel(level).lowercase()} flow for today"
                    },
                    selected = level == selectedLevel,
                    modifier = GlanceModifier.defaultWeight(),
                    action = actionRunCallback<SetFlowAction>(
                        actionParametersOf(SetFlowAction.LevelKey to level)
                    )
                )
            }
        }
    }

    @Composable
    private fun WaterRow(cups: Int) {
        val context = LocalContext.current
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Pill(
                label = if (cups > 0) "+ Water ($cups)" else "+ Water",
                description = "Add one cup of water. $cups logged today",
                selected = false,
                modifier = GlanceModifier.defaultWeight(),
                action = actionRunCallback<AddWaterAction>()
            )
            Spacer(GlanceModifier.width(4.dp))
            Pill(
                label = "More",
                description = "Open LunarLog to log symptoms, mood or a note",
                selected = false,
                modifier = GlanceModifier.defaultWeight(),
                action = deepLinkAction(context, "logging")
            )
        }
    }

    @Composable
    private fun Pill(
        label: String,
        description: String,
        selected: Boolean,
        modifier: GlanceModifier,
        action: Action
    ) {
        Box(
            modifier = modifier
                .background(if (selected) WidgetColors.primary else WidgetColors.container)
                .cornerRadius(14.dp)
                .padding(vertical = 7.dp, horizontal = 2.dp)
                .semantics { contentDescription = description }
                .clickable(action),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = if (selected) WidgetColors.onPrimary else WidgetColors.onSurface,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    private companion object {
        /** 4x1 is the intended shape; 4x2 adds the water row. */
        val COMPACT = DpSize(250.dp, 40.dp)
        val TALL = DpSize(250.dp, 110.dp)

        /** `DailyLog.flowLevel` values, excluding 0 which means "not logged". */
        val FLOW_LEVELS = listOf(1, 2, 3, 4)

        /**
         * Mirrors `ui/util/LogLabels.flowLabel`, but abbreviated: "Medium" does not fit a quarter of a
         * 4-cell-wide widget at a legible size, and Glance cannot ellipsize a centred label gracefully.
         */
        fun flowLabel(level: Int): String = when (level) {
            1 -> "Spot"
            2 -> "Light"
            3 -> "Med"
            4 -> "Heavy"
            else -> "None"
        }
    }
}

class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}

/**
 * Sets today's flow to the tapped level, or clears it when that level is already the one recorded.
 */
class SetFlowAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val level = parameters[LevelKey] ?: return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val repository = entryPoint.dailyLogRepository()
        val today = LocalDate.now()

        withContext(Dispatchers.IO) {
            runCatching {
                val current = repository.getLogsForRangeSync(today, today).firstOrNull()?.flowLevel ?: 0
                repository.replaceEntriesOfType(
                    date = today.toEpochDay(),
                    type = LogEntryType.FLOW,
                    // Re-tapping the recorded level is the widget's only way to undo a mistap, so it
                    // clears rather than rewriting the same value.
                    values = if (current == level) emptyList() else listOf(level.toString()),
                    time = System.currentTimeMillis()
                )
            }
        }

        // The flow change can start or extend a period in the calendar's eyes, so refresh every
        // widget rather than just this one.
        WidgetRefresher.updateAll(context)
    }

    companion object {
        val LevelKey = ActionParameters.Key<Int>("flow_level")
    }
}

/** Appends one cup of water to today; the daily aggregate sums them. */
class AddWaterAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val today = LocalDate.now()

        withContext(Dispatchers.IO) {
            runCatching {
                entryPoint.dailyLogRepository().addEntry(
                    LogEntry(
                        date = today.toEpochDay(),
                        time = System.currentTimeMillis(),
                        type = LogEntryType.WATER,
                        value = "1"
                    )
                )
            }
        }

        QuickLogWidget().update(context, glanceId)
    }
}
