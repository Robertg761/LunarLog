package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lunarlog.di.WidgetEntryPoint
import com.lunarlog.logic.CyclePhase
import com.lunarlog.logic.CycleRingState
import com.lunarlog.logic.CycleWidgetStateCalculator
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Where today sits in the cycle, as a phase-segmented ring: recorded/expected period days, the
 * estimated fertile window, the estimated ovulation day, and a marker for today.
 *
 * The ring itself is a bitmap — see [CycleRingRenderer] for why, and for the one caveat that
 * carries. Everything layered over and around it is a Glance primitive.
 */
class CycleRingWidget : GlanceAppWidget() {

    /**
     * Three buckets, sized to One UI's home grid (a cell is ~70dp, so `70n - 30` dp for n cells):
     * 2x2 shows the ring and a phase label, 4x2 puts the detail beside it, and 4x4 adds the legend.
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        // A read that throws would otherwise leave the host showing Glance's loading layout forever;
        // an empty cycle list renders the "no data yet" state, which is the honest fallback.
        val cycles = withContext(Dispatchers.IO) {
            runCatching { entryPoint.cycleRepository().getAllCyclesSync() }.getOrDefault(emptyList())
        }
        val state = CycleWidgetStateCalculator.calculate(cycles, LocalDate.now())

        provideContent { RingContent(state) }
    }

    @Composable
    private fun RingContent(state: CycleRingState) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isLarge = size.height >= LARGE.height && size.width >= LARGE.width
        val isWide = !isLarge && size.width >= WIDE.width

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .widgetSurface()
                .padding(if (isLarge) 14.dp else 10.dp)
                .clickable(deepLinkAction(context, "analysis")),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLarge -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ring(state, 116.dp)
                    Spacer(GlanceModifier.height(10.dp))
                    PhaseLabel(state, 15)
                    Supporting(state, 12, maxLines = 2)
                    Spacer(GlanceModifier.height(10.dp))
                    Legend()
                }

                isWide -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(state, 88.dp)
                    Spacer(GlanceModifier.width(14.dp))
                    Column {
                        PhaseLabel(state, 15)
                        Supporting(state, 12, maxLines = 2)
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = cycleDayCaption(state),
                            maxLines = 1,
                            style = TextStyle(color = WidgetColors.muted, fontSize = 11.sp)
                        )
                    }
                }

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ring(state, 74.dp)
                    Spacer(GlanceModifier.height(4.dp))
                    PhaseLabel(state, 12)
                }
            }
        }
    }

    /** The bitmap arcs, with the day number overlaid as text so it stays crisp and theme-aware. */
    @Composable
    private fun Ring(state: CycleRingState, diameter: Dp) {
        val palette = WidgetColors.forContext(LocalContext.current)
        val dayNumberSize = (diameter.value * 0.29f).toInt().coerceIn(16, 40)

        Box(
            modifier = GlanceModifier.size(diameter),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(CycleRingRenderer.render(state, palette)),
                contentDescription = ringDescription(state),
                modifier = GlanceModifier.size(diameter)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.phase == CyclePhase.UNKNOWN) "–" else "${state.cycleDay}",
                    maxLines = 1,
                    style = TextStyle(
                        color = WidgetColors.onSurface,
                        fontSize = dayNumberSize.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (diameter >= 88.dp && state.phase != CyclePhase.UNKNOWN) {
                    Text(
                        text = "of ${state.cycleLength}",
                        maxLines = 1,
                        style = TextStyle(color = WidgetColors.muted, fontSize = 10.sp)
                    )
                }
            }
        }
    }

    @Composable
    private fun PhaseLabel(state: CycleRingState, fontSize: Int) {
        Text(
            text = state.phaseLabel,
            maxLines = 1,
            style = TextStyle(
                color = state.phase.accent(),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }

    @Composable
    private fun Supporting(state: CycleRingState, fontSize: Int, maxLines: Int) {
        if (state.supporting.isEmpty()) return
        Text(
            text = state.supporting,
            maxLines = maxLines,
            style = TextStyle(
                color = WidgetColors.muted,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center
            )
        )
    }

    /** Only shown at 4x4, where there is room to explain what the three arc colours mean. */
    @Composable
    private fun Legend() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendEntry("Period", WidgetColors.periodAccent)
            Spacer(GlanceModifier.width(8.dp))
            LegendEntry("Fertile", WidgetColors.fertileAccent)
            Spacer(GlanceModifier.width(8.dp))
            LegendEntry("Ovulation", WidgetColors.ovulationAccent)
        }
    }

    @Composable
    private fun LegendEntry(label: String, color: ColorProvider) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier.size(8.dp).background(color).cornerRadius(4.dp),
                contentAlignment = Alignment.Center
            ) {}
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.muted, fontSize = 10.sp)
            )
        }
    }

    private fun cycleDayCaption(state: CycleRingState): String =
        if (state.phase == CyclePhase.UNKNOWN) {
            "No cycles recorded"
        } else {
            "Cycle day ${state.cycleDay} of ${state.cycleLength}"
        }

    /**
     * The image carries the whole ring, so its description has to state everything the arcs convey —
     * a screen reader user gets nothing from "cycle ring".
     */
    private fun ringDescription(state: CycleRingState): String =
        if (state.phase == CyclePhase.UNKNOWN) {
            "No cycle data yet"
        } else {
            "Cycle day ${state.cycleDay} of ${state.cycleLength}, ${state.phaseLabel} phase. " +
                state.supporting
        }

    private companion object {
        val COMPACT = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)

        fun CyclePhase.accent(): ColorProvider = when (this) {
            CyclePhase.PERIOD -> WidgetColors.periodAccent
            CyclePhase.FERTILE -> WidgetColors.fertileAccent
            CyclePhase.OVULATION -> WidgetColors.ovulationAccent
            CyclePhase.FOLLICULAR, CyclePhase.LUTEAL -> WidgetColors.primary
            CyclePhase.UNKNOWN -> WidgetColors.muted
        }
    }
}

class CycleRingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CycleRingWidget()
}
