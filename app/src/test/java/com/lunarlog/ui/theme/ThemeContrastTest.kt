package com.lunarlog.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private val seeds = listOf(
        Color(0xFFD93672),
        Color(0xFFF26399),
        Color(0xFFE1BEE7),
        Color(0xFFFFCCBC),
        Color(0xFFB2DFDB),
        Color(0xFFBBDEFB)
    )

    @Test
    fun `preset seed themes choose readable primary content colors`() {
        seeds.forEach { seed ->
            val light = generateLightSchemeFromSeed(seed)
            val dark = generateDarkSchemeFromSeed(seed)

            assertTrue(contrastRatio(light.primary, light.onPrimary) >= 4.5f)
            assertTrue(contrastRatio(light.primaryContainer, light.onPrimaryContainer) >= 4.5f)
            assertTrue(contrastRatio(light.secondary, light.onSecondary) >= 4.5f)
            assertTrue(contrastRatio(light.secondaryContainer, light.onSecondaryContainer) >= 4.5f)
            assertTrue(contrastRatio(light.tertiary, light.onTertiary) >= 4.5f)
            assertTrue(contrastRatio(dark.primary, dark.onPrimary) >= 4.5f)
            assertTrue(contrastRatio(dark.primaryContainer, dark.onPrimaryContainer) >= 4.5f)
            assertTrue(contrastRatio(dark.secondary, dark.onSecondary) >= 4.5f)
            assertTrue(contrastRatio(dark.secondaryContainer, dark.onSecondaryContainer) >= 4.5f)
            assertTrue(contrastRatio(dark.tertiary, dark.onTertiary) >= 4.5f)
        }
    }
}
