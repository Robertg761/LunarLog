package com.lunarlog.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrast floors for the semantic cycle palette.
 *
 * [ThemeContrastTest] covers the Material roles the seed generator derives; these are the
 * hand-picked values in `Color.kt`, which nothing else checks. They are easy to nudge for looks and
 * hard to notice having broken, because the states they mark (fertile window, ovulation) only show
 * up with the right data on screen.
 */
class CycleColorContrastTest {

    private fun assertReadable(container: Color, onContainer: Color, label: String) {
        val ratio = contrastRatio(container, onContainer)
        assertTrue("$label text is $ratio:1, below the 4.5:1 floor", ratio >= 4.5f)
    }

    private fun assertVisibleMark(accent: Color, against: Color, label: String) {
        val ratio = contrastRatio(accent, against)
        assertTrue("$label mark is $ratio:1, below WCAG's 3:1 for non-text", ratio >= 3f)
    }

    @Test
    fun `on-container text is readable on every cycle fill`() {
        assertReadable(PeriodSurface, OnPeriodSurface, "light period")
        assertReadable(FertileSurface, OnFertileSurface, "light fertile")
        assertReadable(OvulationSurface, OnOvulationSurface, "light ovulation")
        assertReadable(PeriodSurfaceDark, OnPeriodSurfaceDark, "dark period")
        assertReadable(FertileSurfaceDark, OnFertileSurfaceDark, "dark fertile")
        assertReadable(OvulationSurfaceDark, OnOvulationSurfaceDark, "dark ovulation")
        assertReadable(LightCycleColors.periodStrong, LightCycleColors.onPeriodStrong, "period FAB")
        assertReadable(DarkCycleColors.periodStrong, DarkCycleColors.onPeriodStrong, "dark period FAB")
    }

    @Test
    fun `marks drawn on the page background clear the non-text minimum`() {
        assertVisibleMark(LightCycleColors.period, SurfaceLight, "light period")
        assertVisibleMark(LightCycleColors.fertile, SurfaceLight, "light fertile")
        assertVisibleMark(LightCycleColors.ovulation, SurfaceLight, "light ovulation")
        assertVisibleMark(LightCycleColors.today, SurfaceLight, "light today ring")
        assertVisibleMark(DarkCycleColors.period, BackgroundDark, "dark period")
        assertVisibleMark(DarkCycleColors.fertile, BackgroundDark, "dark fertile")
        assertVisibleMark(DarkCycleColors.ovulation, BackgroundDark, "dark ovulation")
        assertVisibleMark(DarkCycleColors.today, BackgroundDark, "dark today ring")
    }

    /**
     * The dark fills used to sit at 1.16–1.36:1 against the background — near-black on near-black,
     * with no hue separation to rescue them either, so a marked day read as a hole in the grid.
     *
     * Deliberately not asserted for light mode: those pastels are around 1.05:1 and are perfectly
     * legible, because pale blue and pale green on pale pink separate by hue instead. Luminance
     * contrast is only the whole story when the two colours are close in hue, which is exactly the
     * case dark mode was failing.
     */
    @Test
    fun `dark cycle fills read as shapes against the dark background`() {
        listOf(
            "period" to PeriodSurfaceDark,
            "fertile" to FertileSurfaceDark,
            "ovulation" to OvulationSurfaceDark
        ).forEach { (label, fill) ->
            val ratio = contrastRatio(fill, BackgroundDark)
            assertTrue("dark $label fill is $ratio:1 against the background", ratio >= 1.5f)
        }
    }
}
