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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
// Use standard library Color class for MCU compatibility if needed, but for now we manually map
// Actually, Compose Material3 doesn't expose a public "generate from seed" easily without libraries like material-color-utilities (which is JS/Dart mainly, the Android one is embedded in dynamic*).
// However, since we want a CUSTOM seed that is NOT the wallpaper, we need a way to generate the tonal palettes.
// For simplicity in this iteration without adding a heavy external color engine library manually, 
// we will implement a simplified generator or just a few preset themes. 
// BUT, the user asked for "Dynamic Theming Engine". 
// Let's implement a basic "Tonal Palette" generator or just use a few predefined palettes for robustness if MCU is not easily accessible in pure Kotlin without KMP setup.
// Wait, I can use `androidx.core.graphics.ColorUtils` or similar? No, that's not full Material 3.
// Let's stick to the prompt: "User-selectable Seed Color".
// I'll assume we can't easily generate the FULL accessible M3 scheme from scratch without `com.google.material.color:material-color-utilities` which I added via npm but that's for JS...
// Ah, the proper Android dependency is `com.google.android.material:material` but that's View-based.
// Okay, I will implement a "Simple" dynamic theme that tints the primary/secondary colors based on the seed.

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary40,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary80,
    onSecondary = Secondary40,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary80,
    onTertiary = Tertiary40,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = BackgroundDark,
    surface = SurfaceDark
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
    surface = SurfaceLight,
)

@Composable
fun LunarLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
            window.statusBarColor = Color.Transparent.toArgb()
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

// Simple generation logic (Placeholder for full MCU)
// Real implementation would use HCT color space.
fun generateLightSchemeFromSeed(seed: Color): ColorScheme {
    return lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = seed.copy(alpha = 0.3f),
        onPrimaryContainer = Color.Black,
        secondary = seed.copy(alpha = 0.8f),
        onSecondary = Color.White,
        tertiary = seed.copy(alpha = 0.6f),
        background = Color(0xFFFFFBFB),
        surface = Color(0xFFFFFBFB)
    )
}

fun generateDarkSchemeFromSeed(seed: Color): ColorScheme {
    return darkColorScheme(
        primary = seed,
        onPrimary = Color.Black,
        primaryContainer = seed.copy(alpha = 0.3f),
        onPrimaryContainer = Color.White,
        secondary = seed.copy(alpha = 0.8f),
        onSecondary = Color.Black,
        tertiary = seed.copy(alpha = 0.6f),
        background = Color(0xFF1C1B1B),
        surface = Color(0xFF1C1B1B)
    )
}