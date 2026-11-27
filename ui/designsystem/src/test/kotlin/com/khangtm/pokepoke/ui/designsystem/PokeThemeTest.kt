package com.khangtm.pokepoke.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class PokeThemeTest {

    @Test
    fun `light and dark schemes use the brand colors`() {
        assertEquals(Color(0xFFB8401F), PokeLightColorScheme.primary)
        assertEquals(Color(0xFFFFF8F6), PokeLightColorScheme.surface)
        assertEquals(Color(0xFFFFFFFF), PokeLightColorScheme.surfaceContainerLowest)
        assertEquals(Color(0xFFFFB5A0), PokeDarkColorScheme.primary)
        assertEquals(Color(0xFF1A110F), PokeDarkColorScheme.background)
        assertEquals(Color(0xFF3D3230), PokeDarkColorScheme.surfaceContainerHighest)
    }

    @Test
    fun `theme exposes the dark scheme when darkTheme is true and light otherwise`() = runComposeUiTest {
        var primaryLight: Color? = null
        var primaryDark: Color? = null
        setContent {
            PokeTheme(darkTheme = false) { primaryLight = MaterialTheme.colorScheme.primary }
            PokeTheme(darkTheme = true) { primaryDark = MaterialTheme.colorScheme.primary }
        }
        waitForIdle()
        assertEquals(PokeLightColorScheme.primary, primaryLight)
        assertEquals(PokeDarkColorScheme.primary, primaryDark)
    }

    @Test
    fun `typography overrides weights`() {
        assertEquals(FontWeight.Bold, PokeTypography.headlineMedium.fontWeight)
        assertEquals(FontWeight.Bold, PokeTypography.titleLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, PokeTypography.labelLarge.fontWeight)
    }

    @Test
    fun `shapes and sizes follow the design system`() {
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(28.dp), PokeShapes.extraLarge)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(16.dp), PokeShapes.large)
        assertEquals(48.dp, PokeSizes.minTouchTarget)
        assertEquals(600.dp, PokeSizes.maxContentWidth)
        assertEquals(12.dp, PokeSpacing.md)
    }
}
