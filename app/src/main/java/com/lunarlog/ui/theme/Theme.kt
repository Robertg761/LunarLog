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
    surfaceContainer = SurfaceContainerDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFE5C1CC),
    outline = OutlineDark,
    outlineVariant = Color(0xFF6C4A58)
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
    surfaceContainer = SurfaceContainerLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF72535F),
    outline = OutlineLight,
    outlineVariant = Color(0xFFE3C3CE)
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

fun generateLightSchemeFromSeed(seed: Color): ColorScheme {
    val primary = seed.copy(alpha = 1f)
    val secondary = lerp(primary, BrandPlum, 0.18f)
    val tertiary = lerp(primary, Color(0xFF6750A4), 0.35f)
    val primaryContainer = lerp(primary, Color.White, 0.78f)
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
        surfaceContainer = SurfaceContainerLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = Color(0xFF72535F),
        outline = OutlineLight
    )
}

fun generateDarkSchemeFromSeed(seed: Color): ColorScheme {
    val primary = lerp(seed.copy(alpha = 1f), Color.White, 0.28f)
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
        surfaceContainer = SurfaceContainerDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = Color(0xFFE5C1CC),
        outline = OutlineDark
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
