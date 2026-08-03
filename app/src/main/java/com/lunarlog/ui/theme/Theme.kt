package com.lunarlog.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = BrandPlum,
    primaryContainer = BrandRoseDeep,
    onPrimaryContainer = Color(0xFFFFD7E4),
    secondary = Secondary80,
    onSecondary = BrandPlum,
    secondaryContainer = Color(0xFF5F253B),
    onSecondaryContainer = Color(0xFFFFD8E5),
    tertiary = Tertiary80,
    onTertiary = BrandPlum,
    tertiaryContainer = Color(0xFF63344A),
    onTertiaryContainer = Color(0xFFFFD8E7),
    background = BackgroundDark,
    onBackground = Color(0xFFFFECF2),
    surface = SurfaceDark,
    onSurface = Color(0xFFFFECF2),
    // The whole ladder, not just `surfaceContainer` — see the note in Color.kt.
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFE5C1CC),
    outline = OutlineDark,
    outlineVariant = Color(0xFF6C4A58),
    // Snackbars paint from the inverse roles; without these M3 falls back to its baseline
    // neutral grey with a lavender action label, which reads as a different app.
    inverseSurface = Color(0xFFFFECF2),
    inverseOnSurface = BrandInk,
    inversePrimary = Primary40
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary40,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary40,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = BackgroundLight,
    onBackground = BrandInk,
    surface = SurfaceLight,
    onSurface = BrandInk,
    // The whole ladder, not just `surfaceContainer` — see the note in Color.kt.
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF72535F),
    outline = OutlineLight,
    outlineVariant = Color(0xFFE3C3CE),
    inverseSurface = BrandInk,
    inverseOnSurface = Color(0xFFFFECF2),
    inversePrimary = Primary80
)

@Composable
fun LunarLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    seedColor: Int? = null, // New parameter
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        seedColor != null -> {
            // Generate scheme from seed (Simple Hue Shift for now)
            val seed = Color(seedColor)
            if (darkTheme) generateDarkSchemeFromSeed(seed) else generateLightSchemeFromSeed(seed)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Keyed off the `darkTheme` parameter rather than a fresh isSystemInDarkTheme() call, so
    // previews and any future in-app override can't desync the cycle colours from the scheme.
    CompositionLocalProvider(
        LocalCycleColors provides if (darkTheme) DarkCycleColors else LightCycleColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Walks [seed]'s HSL lightness ([darken] down, otherwise up) only as far as it takes to clear
 * [minRatio] against [against].
 *
 * The shipped theme swatches are M3 tone-200 pastels: used raw as `primary` they land near 1.4:1
 * on the app surface, which erases every section header and selected nav label. Stepping lightness
 * rather than lerping toward ink/white keeps the hue and saturation the user actually picked — a
 * lerp turns the pale blue swatch into slate grey by the time it reaches 4.5:1.
 *
 * Terminates either on the ratio or at L=0 / L=1, both of which clear 4.5:1 against the app's
 * surfaces, so there is always a usable result.
 */
private fun toneMapSeed(
    seed: Color,
    against: Color,
    darken: Boolean,
    minRatio: Float = 4.5f
): Color {
    val opaque = seed.copy(alpha = 1f)
    if (contrastRatio(opaque, against) >= minRatio) return opaque

    val (hue, saturation, seedLightness) = opaque.toHsl()
    val step = if (darken) -0.02f else 0.02f
    var lightness = seedLightness
    var mapped = opaque
    while (lightness > 0f && lightness < 1f) {
        lightness = (lightness + step).coerceIn(0f, 1f)
        mapped = hslToColor(hue, saturation, lightness)
        if (contrastRatio(mapped, against) >= minRatio) break
    }
    return mapped
}

/**
 * RGB to HSL: hue in degrees, saturation and lightness in 0..1.
 *
 * Hand-rolled rather than `androidx.core.graphics.ColorUtils`, which delegates to
 * `android.graphics.Color` and so throws "not mocked" the moment a plain JVM unit test touches the
 * theme — `ThemeContrastTest` builds every preset seed's scheme on the JVM.
 */
private fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2f
    val delta = max - min
    if (delta == 0f) return Triple(0f, 0f, lightness)

    val saturation = delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return Triple(if (hue < 0f) hue + 360f else hue, saturation.coerceIn(0f, 1f), lightness)
}

/** The inverse of [toHsl]. */
private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val x = c * (1f - kotlin.math.abs((h % 2f) - 1f))
    val (r1, g1, b1) = when {
        h < 1f -> Triple(c, x, 0f)
        h < 2f -> Triple(x, c, 0f)
        h < 3f -> Triple(0f, c, x)
        h < 4f -> Triple(0f, x, c)
        h < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = lightness - c / 2f
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f)
    )
}

fun generateLightSchemeFromSeed(seed: Color): ColorScheme {
    // The raw seed still drives the containers and fills below, so the picked colour stays visible.
    val primary = toneMapSeed(seed, against = SurfaceLight, darken = true)
    val secondary = lerp(primary, BrandPlum, 0.18f)
    val tertiary = lerp(primary, Color(0xFF6750A4), 0.35f)
    val primaryContainer = lerp(seed.copy(alpha = 1f), Color.White, 0.78f)
    val secondaryContainer = lerp(secondary, Color.White, 0.82f)
    return lightColorScheme(
        primary = primary,
        onPrimary = bestContentColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = bestContentColor(primaryContainer),
        secondary = secondary,
        onSecondary = bestContentColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = bestContentColor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = bestContentColor(tertiary),
        background = BackgroundLight,
        onBackground = BrandInk,
        surface = SurfaceLight,
        onSurface = BrandInk,
        // Kept in step with LightColorScheme; see the note in Color.kt.
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = Color(0xFF72535F),
        outline = OutlineLight,
        // Kept in step with LightColorScheme, otherwise picking any swatch silently flips every
        // divider from the warm rose outline to M3's grey-lilac baseline.
        outlineVariant = Color(0xFFE3C3CE),
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,
        inverseSurface = BrandInk,
        inverseOnSurface = Color(0xFFFFECF2),
        inversePrimary = Primary80
    )
}

fun generateDarkSchemeFromSeed(seed: Color): ColorScheme {
    val primary = toneMapSeed(
        lerp(seed.copy(alpha = 1f), Color.White, 0.28f),
        against = SurfaceDark,
        darken = false
    )
    val secondary = lerp(seed.copy(alpha = 1f), Color.White, 0.4f)
    val tertiary = lerp(seed.copy(alpha = 1f), Color(0xFFD0BCFF), 0.45f)
    val primaryContainer = lerp(seed.copy(alpha = 1f), BackgroundDark, 0.55f)
    val secondaryContainer = lerp(seed.copy(alpha = 1f), BackgroundDark, 0.68f)
    return darkColorScheme(
        primary = primary,
        onPrimary = bestContentColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = bestContentColor(primaryContainer),
        secondary = secondary,
        onSecondary = bestContentColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = bestContentColor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = bestContentColor(tertiary),
        background = BackgroundDark,
        onBackground = Color(0xFFFFECF2),
        surface = SurfaceDark,
        onSurface = Color(0xFFFFECF2),
        // Kept in step with DarkColorScheme; see the note in Color.kt.
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = Color(0xFFE5C1CC),
        outline = OutlineDark,
        outlineVariant = Color(0xFF6C4A58),
        tertiaryContainer = Color(0xFF63344A),
        onTertiaryContainer = Color(0xFFFFD8E7),
        inverseSurface = Color(0xFFFFECF2),
        inverseOnSurface = BrandInk,
        inversePrimary = Primary40
    )
}

internal fun bestContentColor(background: Color): Color =
    if (contrastRatio(background, Color.Black) >= contrastRatio(background, Color.White)) {
        Color.Black
    } else {
        Color.White
    }

internal fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
