package com.khangtm.pokepoke.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val BaseTypography = Typography()

/** Material 3 default type scale, system font, with a few heavier weights. */
val PokeTypography: Typography = BaseTypography.copy(
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
