package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lunarlog.R
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
        val primaryColor = ColorProvider(Color(0xFFD81B60))
        val onPrimaryColor = ColorProvider(Color.White)
        val surfaceColor = ColorProvider(Color.White)
        val subtitleColor = ColorProvider(Color(0xFF616161))
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = counter.title,
                        style = TextStyle(color = primaryColor)
                    )
                    Text(
                        text = "${counter.value}",
                        style = TextStyle(
                            fontSize = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                }
                
                Spacer(GlanceModifier.width(16.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = counter.subtitle,
                        style = TextStyle(color = subtitleColor)
                    )
                }
            }

            Spacer(GlanceModifier.height(12.dp))

            // Action Button
            Row(
                modifier = GlanceModifier
                    .background(primaryColor)
                    .padding(8.dp)
                    .clickable(actionRunCallback<LogPeriodAction>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // We don't have easy vector icon support in glance without resources, text is safer
                Text(
                    text = "+ Log Period",
                    style = TextStyle(color = onPrimaryColor, fontWeight = FontWeight.Medium)
                )
            }
        }
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
        
        // Refresh widget
        LogPeriodWidget().update(context, glanceId)
    }
}
