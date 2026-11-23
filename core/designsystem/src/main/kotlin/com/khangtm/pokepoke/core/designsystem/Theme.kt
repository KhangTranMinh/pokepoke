package com.khangtm.pokepoke.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** App theme: Material 3, fixed brand colors (no dynamic color), light / dark follows the system by default. */
@Composable
fun PokeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PokeDarkColorScheme else PokeLightColorScheme,
        typography = PokeTypography,
        shapes = PokeShapes,
        content = content,
    )
}
