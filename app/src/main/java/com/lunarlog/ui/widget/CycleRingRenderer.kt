package com.lunarlog.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.lunarlog.logic.CycleRingState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the cycle ring as a bitmap, because Glance cannot draw arcs.
 *
 * Glance's only progress primitives are `LinearProgressIndicator` and an *indeterminate*
 * `CircularProgressIndicator`, so a determinate ring has to come from a [Canvas] and travel to the
 * host inside the RemoteViews as a bitmap. Two consequences shape everything below:
 *
 *  - **Payload size.** RemoteViews have a ~1MB transaction budget and One UI is stricter about it
 *    than AOSP. [SIZE_PX] is a fixed 168px rather than anything density-derived (a 3x xxhdpi ring
 *    would be 360px, ~518KB at ARGB_8888) and the config is [Bitmap.Config.RGB_565], which halves it
 *    again to ~56KB. Alpha is not needed because the ring is painted onto its own opaque copy of the
 *    widget surface. The host scales the result up; flat arcs survive that fine.
 *  - **Theme staleness.** A bitmap cannot re-resolve itself the way `ColorProvider(day, night)` can,
 *    so the ring is rendered against whichever [WidgetPalette] was current at update time. Toggling
 *    system dark mode therefore leaves the arcs on the previous palette until the next refresh
 *    (midnight, a data change, or a tap). Only the ring has this caveat — every text and fill around
 *    it is a Glance primitive and adapts immediately. Keeping the bitmap to just the arcs is the
 *    reason the day number and labels are Glance `Text` overlaid in a `Box` rather than drawn here.
 */
internal object CycleRingRenderer {

    private const val SIZE_PX = 168
    private const val STROKE_FRACTION = 0.115f

    /** Canvas angles run clockwise from 3 o'clock; cycle day 1 should start at the top. */
    private const val TOP_ANGLE = -90f

    private const val DEGREES = 360f

    fun render(state: CycleRingState, palette: WidgetPalette): Bitmap {
        val bitmap = createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.surface)

        val stroke = SIZE_PX * STROKE_FRACTION
        val inset = stroke / 2f + 1f
        val bounds = RectF(inset, inset, SIZE_PX - inset, SIZE_PX - inset)

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.BUTT
        }

        // Track first, so a cycle with no placeable arcs still reads as a ring rather than a gap.
        arcPaint.color = palette.container
        canvas.drawOval(bounds, arcPaint)

        val cycleLength = state.cycleLength.coerceAtLeast(1)

        arcPaint.color = palette.periodAccent
        drawDayRange(canvas, bounds, arcPaint, state.periodDays, cycleLength)

        arcPaint.color = palette.fertileAccent
        drawDayRange(canvas, bounds, arcPaint, state.fertileDays, cycleLength)

        // Ovulation last of the arcs: it sits inside the fertile window and must paint over it.
        state.ovulationDay?.let { day ->
            arcPaint.color = palette.ovulationAccent
            drawDayRange(canvas, bounds, arcPaint, day..day, cycleLength)
        }

        if (state.cycleDay > 0) {
            drawTodayMarker(canvas, bounds, stroke, state, cycleLength, palette)
        }

        return bitmap
    }

    private fun drawDayRange(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        days: IntRange,
        cycleLength: Int
    ) {
        if (days.isEmpty()) return
        val startAngle = TOP_ANGLE + (days.first - 1).toFloat() / cycleLength * DEGREES
        val sweep = (days.last - days.first + 1).toFloat() / cycleLength * DEGREES
        canvas.drawArc(bounds, startAngle, sweep, false, paint)
    }

    /**
     * A dot on the ring path marking today, ringed in the surface colour so it separates from
     * whichever arc it lands on.
     */
    private fun drawTodayMarker(
        canvas: Canvas,
        bounds: RectF,
        stroke: Float,
        state: CycleRingState,
        cycleLength: Int,
        palette: WidgetPalette
    ) {
        // An overdue period pushes cycleDay past the end of the ring; pin it to the last slot so the
        // marker stays on the circle instead of wrapping around to the start.
        val day = min(state.cycleDay, cycleLength)
        val midAngle = TOP_ANGLE + (day - 0.5f) / cycleLength * DEGREES
        val radians = Math.toRadians(midAngle.toDouble())
        val radius = bounds.width() / 2f
        val cx = bounds.centerX() + radius * cos(radians).toFloat()
        val cy = bounds.centerY() + radius * sin(radians).toFloat()

        val dotRadius = stroke * 0.62f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        paint.color = palette.surface
        canvas.drawCircle(cx, cy, dotRadius, paint)
        paint.color = palette.onSurface
        canvas.drawCircle(cx, cy, dotRadius * 0.62f, paint)
    }
}
