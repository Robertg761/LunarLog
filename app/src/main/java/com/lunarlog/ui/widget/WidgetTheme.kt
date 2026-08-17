package com.lunarlog.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider
// The day/night factory, which is a *function* named ColorProvider in a different package from the
// ColorProvider *interface* imported above. Kotlin keeps types and functions in separate namespaces,
// so both names coexist; the import above is the type the properties below are declared as.
import androidx.glance.color.ColorProvider

/**
 * One theme-resolved LunarLog palette as raw ARGB ints.
 *
 * Glance's `ColorProvider(day, night)` covers everything drawn with Glance primitives, but the cycle
 * ring is a [android.graphics.Canvas] bitmap and `Paint` needs plain ints. Rather than keep two
 * hand-synced colour lists, the ints are the source of truth and the Glance providers in
 * [WidgetColors] are derived from them.
 *
 * Values mirror `ui/theme/Color.kt` so a widget and the app screen behind it agree on what "period"
 * or "fertile" looks like.
 */
internal data class WidgetPalette(
    val surface: Int,
    val onSurface: Int,
    /** Secondary text — subtitles, weekday headers, out-of-month dates. */
    val muted: Int,
    val outline: Int,
    val primary: Int,
    val onPrimary: Int,
    /** Chip/button fill for neutral affordances. */
    val container: Int,
    val period: Int,
    val onPeriod: Int,
    val predictedPeriod: Int,
    val fertile: Int,
    val onFertile: Int,
    val ovulation: Int,
    val onOvulation: Int,
    val today: Int,
    // The `*Accent` trio is for marks drawn *directly on* [surface] — the ring's arcs. The pale
    // containers above are only legible with text on top of them, which is what the calendar's day
    // cells do; on a bare near-white surface FertileSurface sits at 1.05:1 and disappears. Light
    // values are therefore the 600-weights `Color.kt` keeps for exactly this case.
    val periodAccent: Int,
    val fertileAccent: Int,
    val ovulationAccent: Int
)

internal object WidgetColors {

    /** Light values: SurfaceLight, BrandInk, BrandRoseDeep, and the `*Surface` cycle containers. */
    val Light = WidgetPalette(
        surface = 0xFFFFFAFB.toInt(),
        onSurface = 0xFF251018.toInt(),
        muted = 0xFF72535F.toInt(),
        outline = 0xFF8E6C78.toInt(),
        primary = 0xFFA81852.toInt(),
        onPrimary = 0xFFFFFFFF.toInt(),
        container = 0xFFF7E3EA.toInt(),
        period = 0xFFFFD9E2.toInt(),
        onPeriod = 0xFF8C0032.toInt(),
        predictedPeriod = 0xFFF7DDE6.toInt(),
        fertile = 0xFFE8F5E9.toInt(),
        onFertile = 0xFF1B5E20.toInt(),
        ovulation = 0xFFE3F2FD.toInt(),
        onOvulation = 0xFF0D47A1.toInt(),
        // TodayRingLight rather than TodayRing: the pale gold only clears 3:1 on a dark surface.
        today = 0xFFC77800.toInt(),
        periodAccent = 0xFFD93672.toInt(),
        fertileAccent = 0xFF43A047.toInt(),
        ovulationAccent = 0xFF1E88E5.toInt()
    )

    /** Dark values: SurfaceDark, BrandRoseLight, and the `*SurfaceDark` cycle containers. */
    val Dark = WidgetPalette(
        surface = 0xFF201018.toInt(),
        onSurface = 0xFFFFE4EC.toInt(),
        muted = 0xFFE5C1CC.toInt(),
        outline = 0xFFB99AA6.toInt(),
        primary = 0xFFF26399.toInt(),
        onPrimary = 0xFF3D1024.toInt(),
        container = 0xFF371E2A.toInt(),
        period = 0xFF792234.toInt(),
        onPeriod = 0xFFFFD9E2.toInt(),
        predictedPeriod = 0xFF533241.toInt(),
        fertile = 0xFF164C16.toInt(),
        onFertile = 0xFFC8E6C9.toInt(),
        ovulation = 0xFF28426C.toInt(),
        onOvulation = 0xFFBBDEFB.toInt(),
        today = 0xFFFFB74D.toInt(),
        periodAccent = 0xFFF26399.toInt(),
        fertileAccent = 0xFF81C784.toInt(),
        ovulationAccent = 0xFF64B5F6.toInt()
    )

    /**
     * The palette matching the host's current night-mode setting.
     *
     * Only the canvas ring needs this. Everything drawn with Glance primitives should use the
     * providers below, which the host re-resolves on its own and therefore survive a dark-mode
     * toggle without a widget update.
     */
    fun forContext(context: Context): WidgetPalette {
        val night = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (night) Dark else Light
    }

    // `ColorProvider(R.color.x)` is the other obvious way to write these, but that overload is
    // @RestrictedApi to Glance's own library group — calling it is a lint *error* and fails the
    // release gate. This is the public day/night API, which is also why there is no widget colour
    // resource file.
    private fun provider(selector: (WidgetPalette) -> Int): ColorProvider =
        ColorProvider(day = Color(selector(Light)), night = Color(selector(Dark)))

    val surface = provider { it.surface }
    val onSurface = provider { it.onSurface }
    val muted = provider { it.muted }
    val outline = provider { it.outline }
    val primary = provider { it.primary }
    val onPrimary = provider { it.onPrimary }
    val container = provider { it.container }
    val period = provider { it.period }
    val onPeriod = provider { it.onPeriod }
    val predictedPeriod = provider { it.predictedPeriod }
    val fertile = provider { it.fertile }
    val onFertile = provider { it.onFertile }
    val ovulation = provider { it.ovulation }
    val onOvulation = provider { it.onOvulation }
    val today = provider { it.today }
    val periodAccent = provider { it.periodAccent }
    val fertileAccent = provider { it.fertileAccent }
    val ovulationAccent = provider { it.ovulationAccent }
}

/**
 * `android.R.dimen.system_app_widget_background_radius` only exists on API 31+; below that fall back
 * to a fixed radius (where Glance's corner radius support is itself a no-op anyway).
 */
internal fun GlanceModifier.widgetCornerRadius(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        cornerRadius(16.dp)
    }

/**
 * The opaque, corner-masked card every LunarLog widget sits on.
 *
 * Opaque on purpose. One UI's "widget transparency" and theming pass tints anything that leaves its
 * background unset, which on Samsung shows up as a widget that inverts with the wallpaper instead of
 * following the app's own palette. Painting the surface ourselves also matters on Android 12+, where
 * the host clips widgets to the system corner radius — a square surface shows light wedges at each
 * corner unless it matches the mask.
 */
internal fun GlanceModifier.widgetSurface(): GlanceModifier =
    this.background(WidgetColors.surface).widgetCornerRadius()
