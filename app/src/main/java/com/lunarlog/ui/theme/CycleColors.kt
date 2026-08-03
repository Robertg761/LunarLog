package com.lunarlog.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware accessors for LunarLog's semantic cycle colours.
 *
 * [Color.kt] ships a light *and* a dark value for every cycle state, but historically only
 * `CalendarScreen` branched on the theme; every other screen picked the light value
 * unconditionally, so period/fertile panels stayed pastel-on-dark in dark mode.
 *
 * Read these through [LocalCycleColors] (or [cycleColors]) instead of importing the raw values
 * from [Color.kt], so a cycle state is coloured the same way everywhere and stays correct in
 * both themes. [LunarLogTheme] provides the right set based on its own `darkTheme` flag, which
 * also keeps this correct if an in-app theme override is added later.
 *
 * Naming follows Material's container/on-container convention:
 *  - `x`            accent used for icons, rings and marks drawn *on the page background*.
 *                   Chosen to clear 3:1 against the surface, per WCAG non-text contrast.
 *  - `xContainer`   soft fill behind content.
 *  - `onXContainer` content colour for that fill (>= 4.5:1).
 */
@Immutable
data class CycleColors(
    /** Accent for period marks drawn on the page background. */
    val period: Color,
    /** Saturated period fill for filled emphasis (FABs, selected dates). */
    val periodStrong: Color,
    /** Content colour for [periodStrong]. */
    val onPeriodStrong: Color,
    val periodContainer: Color,
    val onPeriodContainer: Color,
    val fertile: Color,
    val fertileContainer: Color,
    val onFertileContainer: Color,
    val ovulation: Color,
    val ovulationContainer: Color,
    val onOvulationContainer: Color,
    /** Ring drawn around "today". */
    val today: Color,
)

/**
 * Light-theme cycle colours.
 *
 * [FertileGreen] (1.97:1) and [OvulationBlue] (2.16:1) are far too pale to be seen against the
 * near-white surface, so the light accents step down to the 600-weight of the same hue family
 * (3.22:1 and 3.59:1). The pastel values are kept for the soft container fills, where they sit
 * behind dark on-container text and read fine.
 */
val LightCycleColors = CycleColors(
    period = BrandRose,
    periodStrong = BrandRoseDeep,
    onPeriodStrong = Color.White,
    periodContainer = PeriodSurface,
    onPeriodContainer = OnPeriodSurface,
    fertile = FertileAccentLight,
    fertileContainer = FertileSurface,
    onFertileContainer = OnFertileSurface,
    ovulation = OvulationAccentLight,
    ovulationContainer = OvulationSurface,
    onOvulationContainer = OnOvulationSurface,
    today = TodayRingLight,
)

/**
 * Dark-theme cycle colours.
 *
 * The pastel accents are the *right* choice here: they carry plenty of contrast against the deep
 * plum background, so [FertileGreen]/[OvulationBlue] are used as-is.
 */
val DarkCycleColors = CycleColors(
    period = PeriodRed,
    periodStrong = BrandRoseDeep,
    onPeriodStrong = Color.White,
    periodContainer = PeriodSurfaceDark,
    onPeriodContainer = OnPeriodSurfaceDark,
    fertile = FertileGreen,
    fertileContainer = FertileSurfaceDark,
    onFertileContainer = OnFertileSurfaceDark,
    ovulation = OvulationBlue,
    ovulationContainer = OvulationSurfaceDark,
    onOvulationContainer = OnOvulationSurfaceDark,
    today = TodayRing,
)

val LocalCycleColors = staticCompositionLocalOf { LightCycleColors }

/** Shorthand for [LocalCycleColors]. */
val cycleColors: CycleColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCycleColors.current
