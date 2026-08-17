package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.cornerRadius
import androidx.glance.unit.ColorProvider
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.PeriodChangeResult
import com.lunarlog.data.PeriodChangeAction
import com.lunarlog.di.WidgetEntryPoint
import com.lunarlog.logic.CounterPresentation
import com.lunarlog.logic.CounterPresentationCalculator
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class LogPeriodWidget : GlanceAppWidget() {

    /**
     * Two buckets so the widget lays itself out instead of clipping:
     * - [COMPACT_SIZE] a 2x2-ish cell — counter stacked over a short button.
     * - [WIDE_SIZE] a 4x2-ish cell — counter and subtitle side by side, full button label.
     *
     * Glance picks the largest declared size that fits and reports it through [LocalSize].
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val cycleRepository = entryPoint.cycleRepository()

        val cycles = withContext(Dispatchers.IO) {
            cycleRepository.getAllCyclesSync()
        }

        val counter = CounterPresentationCalculator.calculate(cycles, LocalDate.now())

        provideContent {
            WidgetContent(counter)
        }
    }

    @Composable
    private fun WidgetContent(counter: CounterPresentation) {
        // Glance does not inherit MaterialTheme, so widgets carry their own palette — otherwise this
        // glared white on a dark home screen. It now lives in WidgetTheme.kt, shared with the other
        // four widgets; see there for why the colours are day/night pairs rather than resource ids.
        val primaryColor = WidgetColors.primary
        val onPrimaryColor = WidgetColors.onPrimary
        val subtitleColor = WidgetColors.muted

        val isCompact = LocalSize.current.width < WIDE_SIZE.width

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .widgetSurface()
                .padding(if (isCompact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCompact) {
                CounterBlock(
                    counter = counter,
                    primaryColor = primaryColor,
                    valueFontSize = 28,
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                Text(
                    text = counter.subtitle,
                    maxLines = 2,
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CounterBlock(
                        counter = counter,
                        primaryColor = primaryColor,
                        valueFontSize = 36,
                        horizontalAlignment = Alignment.CenterHorizontally
                    )

                    Spacer(GlanceModifier.width(16.dp))

                    Text(
                        text = counter.subtitle,
                        maxLines = 2,
                        style = TextStyle(color = subtitleColor, textAlign = TextAlign.Center)
                    )
                }
            }

            Spacer(GlanceModifier.height(if (isCompact) 8.dp else 12.dp))

            // Action Button
            val buttonLabel = if (isCompact) "+ Log" else "+ Log Period"
            Row(
                modifier = GlanceModifier
                    .cornerRadius(20.dp)
                    .background(primaryColor)
                    .padding(
                        horizontal = if (isCompact) 12.dp else 16.dp,
                        vertical = if (isCompact) 8.dp else 10.dp
                    )
                    .semantics { contentDescription = "Log a period starting today" }
                    .clickable(actionRunCallback<LogPeriodAction>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // We don't have easy vector icon support in glance without resources, text is safer
                Text(
                    text = buttonLabel,
                    maxLines = 1,
                    style = TextStyle(color = onPrimaryColor, fontWeight = FontWeight.Medium)
                )
            }
        }
    }

    @Composable
    private fun CounterBlock(
        counter: CounterPresentation,
        primaryColor: ColorProvider,
        valueFontSize: Int,
        horizontalAlignment: Alignment.Horizontal
    ) {
        Column(horizontalAlignment = horizontalAlignment) {
            Text(
                text = counter.title,
                maxLines = 1,
                style = TextStyle(color = primaryColor)
            )
            Text(
                text = "${counter.value}",
                maxLines = 1,
                style = TextStyle(
                    fontSize = valueFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            )
        }
    }

    private companion object {
        val COMPACT_SIZE = DpSize(110.dp, 110.dp)
        val WIDE_SIZE = DpSize(250.dp, 110.dp)
    }
}

class LogPeriodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LogPeriodWidget()
}

class LogPeriodAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val cycleRepository = entryPoint.cycleRepository()
        val dailyLogRepository = entryPoint.dailyLogRepository()

        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        
        withContext(Dispatchers.IO) {
            val periodResult = cycleRepository.startPeriod(today)
            if (periodResult is PeriodChangeResult.Success &&
                periodResult.action == PeriodChangeAction.PERIOD_STARTED
            ) {
                dailyLogRepository.addEntry(
                    LogEntry(
                        date = todayEpochDay,
                        time = System.currentTimeMillis(),
                        type = LogEntryType.FLOW,
                        value = "2"
                    )
                )
            }
        }
        
        // Starting a period moves the cycle ring, the calendar marks and this counter all at once, so
        // refresh the whole set rather than just the widget that was tapped.
        WidgetRefresher.updateAll(context)
    }
}
