package com.lunarlog.ui.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
// The day/night factory, which is a *function* named ColorProvider in a different package from the
// ColorProvider *interface* imported above. Kotlin keeps types and functions in separate namespaces,
// so both names coexist; the import above is the type used in the private helpers' signatures.
import androidx.glance.color.ColorProvider
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
        // Glance does not inherit MaterialTheme, so the widget carries its own palette — otherwise
        // it glared white on a dark home screen. These also move it off Material Pink 600 and onto
        // the app's brand rose.
        //
        // `ColorProvider(day, night)` rather than `ColorProvider(R.color.x)`: the resource-id
        // overload is @RestrictedApi to Glance's own library group, so calling it is a lint *error*
        // and fails the release gate. This one is the public day/night API and needs no resources,
        // which is why there is no widget_colors.xml. Values mirror ui/theme/Color.kt —
        // SurfaceLight/SurfaceDark, BrandRoseDeep/BrandRoseLight, and BrandPlum.
        val primaryColor = ColorProvider(day = Color(0xFFA81852), night = Color(0xFFF26399))
        val onPrimaryColor = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF3D1024))
        val surfaceColor = ColorProvider(day = Color(0xFFFFFAFB), night = Color(0xFF201018))
        val subtitleColor = ColorProvider(day = Color(0xFF72535F), night = Color(0xFFE5C1CC))

        val isCompact = LocalSize.current.width < WIDE_SIZE.width

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                // The host clips widgets to the system corner radius on Android 12+, so a square
                // surface shows light wedges at each corner. Match the mask.
                .widgetCornerRadius()
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

/**
 * `android.R.dimen.system_app_widget_background_radius` only exists on API 31+; below that fall back
 * to a fixed radius (where Glance's corner radius support is itself a no-op anyway).
 */
private fun GlanceModifier.widgetCornerRadius(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        cornerRadius(16.dp)
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
