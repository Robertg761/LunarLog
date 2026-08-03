package com.lunarlog.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette sampled from the LunarLog logo.
val BrandRose = Color(0xFFD93672)
val BrandRoseLight = Color(0xFFF26399)
val BrandRoseDeep = Color(0xFFA81852)
val BrandBlush = Color(0xFFFFD4E3)
val BrandMoon = Color(0xFFFFF7FA)
val BrandPetal = Color(0xFFF47BA8)
val BrandPlum = Color(0xFF3D1024)
val BrandInk = Color(0xFF251018)

// Primary - Lunar rose
val Primary80 = Color(0xFFFFB1CF)
val Primary40 = BrandRose
val PrimaryContainer = BrandBlush
val OnPrimaryContainer = BrandPlum

// Secondary - Soft logo highlight
val Secondary80 = Color(0xFFFFB8D0)
val Secondary40 = BrandPetal
val SecondaryContainer = Color(0xFFFFE2EB)
val OnSecondaryContainer = Color(0xFF4B102A)

// Tertiary - Deep berry for contrast and data accents
val Tertiary80 = Color(0xFFEFB8C8)
val Tertiary40 = Color(0xFF8B2C55)
val TertiaryContainer = Color(0xFFFFD8E7)
val OnTertiaryContainer = Color(0xFF3B0820)

// Neutrals & Backgrounds (logo-inspired warm rose)
val SurfaceLight = Color(0xFFFFFAFB)

/**
 * The card/sheet tone in light mode.
 *
 * Was 0xFFFFEFF4, which sits L*95.8 against a L*97.1 background — 1.03:1, so a card was very nearly
 * the page it was drawn on and a screen of stacked cards read as one undifferentiated wash. Dark
 * mode's equivalent pair has always been 1.13:1. This value is the same warm rose hue at M3's
 * intended tone-94, which brings light mode to 1.08:1 and puts the two themes on the same footing.
 */
val SurfaceContainerLight = Color(0xFFFAEAEF)
val SurfaceVariantLight = Color(0xFFF7DDE6)
val SurfaceDark = Color(0xFF201018)
val SurfaceContainerDark = Color(0xFF2A1620)
val SurfaceVariantDark = Color(0xFF533241)
val BackgroundLight = Color(0xFFFFF4F7)
val BackgroundDark = Color(0xFF160B11)

// The rest of M3's surface-container ladder, in the same warm rose hue as the two tones above.
//
// Only `surfaceContainer` used to be defined, so every Material component that paints from one of
// the other four — date pickers and alert dialogs (`surfaceContainerHigh`), Switch's unchecked
// track and M3's own `Card` (`surfaceContainerHighest`), bottom sheets (`surfaceContainerLow`) —
// fell back to the baseline lilac grey and read as a different app dropped into a pink one.
//
// Steps are evenly spaced in CIE L* (light: 100/96/94/92/90, dark: 4/8.5/10.4/15/20) at the hue the
// existing surfaces already use, so elevation reads in the right direction and every step is the
// same perceptual size — rather than the baseline neutral's lilac.
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFFDF0F5)
val SurfaceContainerHighLight = Color(0xFFF7E3EA)
val SurfaceContainerHighestLight = Color(0xFFF4DCE5)
val SurfaceContainerLowestDark = Color(0xFF160B10)
val SurfaceContainerLowDark = Color(0xFF25131C)
val SurfaceContainerHighDark = Color(0xFF371E2A)
val SurfaceContainerHighestDark = Color(0xFF452736)

val OutlineLight = Color(0xFF8E6C78)
val OutlineDark = Color(0xFFB99AA6)

// Semantic Colors - Refined for "Beautiful" UI
val PeriodRed = BrandRoseLight

val PeriodSurface = Color(0xFFFFD9E2) // Light pink background for period days
val OnPeriodSurface = Color(0xFF8C0032) // Dark text for period days

// The three dark cycle containers below sit at L*28 rather than the L*11–18 they used to.
//
// They are fills whose *shape* carries meaning — a marked day in the calendar grid, the fertility
// panel on Home — and at their old tones they were 1.16:1 to 1.36:1 against the dark background.
// Their light-mode counterparts are no better on paper (1.05:1 for the fertile pastel) but get
// away with it, because pale blue and pale green on pale pink separate by hue even when they don't
// separate by lightness. Near-black navy on near-black plum separates by neither, so the ovulation
// day read as a hole punched in the grid rather than a marked date. At L*28 each is ~1.9:1 against
// the background and the light on-container text still clears 7:1. Hue is unchanged.
val PeriodSurfaceDark = Color(0xFF792234) // Deep rose fill for Dark Mode
val OnPeriodSurfaceDark = Color(0xFFFFD9E2) // Light Pink text for Dark Mode

val FertileGreen = Color(0xFF81C784) // Soft Green — reads on dark surfaces only (1.97:1 on light)
val FertileSurface = Color(0xFFE8F5E9)
val OnFertileSurface = Color(0xFF1B5E20)
val FertileSurfaceDark = Color(0xFF164C16) // Deep green fill for Dark Mode
val OnFertileSurfaceDark = Color(0xFFC8E6C9)

val OvulationBlue = Color(0xFF64B5F6) // Soft Blue — reads on dark surfaces only (2.16:1 on light)
val OvulationSurface = Color(0xFFE3F2FD)
val OnOvulationSurface = Color(0xFF0D47A1)
val OvulationSurfaceDark = Color(0xFF28426C) // Deep blue fill for Dark Mode
val OnOvulationSurfaceDark = Color(0xFFBBDEFB)

val TodayRing = Color(0xFFFFB74D) // Warm orange/gold for "Today" — dark themes only (1.69:1 on light)

// Light-theme accents. The pastel values above are too pale to clear the 3:1 WCAG
// non-text minimum against the near-white surface, so marks drawn directly on the page
// step down to a deeper weight of the same hue. See CycleColors.kt.
val FertileAccentLight = Color(0xFF43A047)   // 3.22:1 on SurfaceLight
val OvulationAccentLight = Color(0xFF1E88E5) // 3.59:1 on SurfaceLight
val TodayRingLight = Color(0xFFC77800)       // 3.35:1 on SurfaceLight

// Gradients
val GradientPinkStart = Color(0xFFFF80AB)
val GradientPinkEnd = Color(0xFFFF4081)
val GradientPeachStart = Color(0xFFFFCC80)
val GradientPeachEnd = Color(0xFFFFA726)

/**
 * The seed the app themes itself with until the user picks another — the brand rose, [BrandRose].
 *
 * A Long because that is what `UserPreferencesRepository` persists; `themeSeedColor == null` means
 * "never chosen", which resolves to this.
 */
const val DefaultThemeSeedColor: Long = 0xFFD93672L

/**
 * The theme-colour swatches offered in Settings, as `name to ARGB long`.
 *
 * Longs rather than [Color] because this is what `UserPreferencesRepository` persists and what
 * `setThemeSeedColor` takes; the swatch UI converts once at the draw site.
 *
 * The first two are the brand rose and its light tint ([BrandRose] / [BrandRoseLight]); the other
 * four are deliberately left at their original hex values. They are tone-200 pastels that
 * `toneMapSeed()` darkens noticeably, so the chip reads lighter than the theme it applies — but
 * changing a value here would orphan the seed already persisted by anyone who picked it.
 */
val ThemeSwatches: List<Pair<String, Long>> = listOf(
    "Lunar rose" to DefaultThemeSeedColor,
    "Pink" to 0xFFF26399L,
    "Lavender" to 0xFFE1BEE7L,
    "Peach" to 0xFFFFCCBCL,
    "Teal" to 0xFFB2DFDBL,
    "Blue" to 0xFFBBDEFBL
)
