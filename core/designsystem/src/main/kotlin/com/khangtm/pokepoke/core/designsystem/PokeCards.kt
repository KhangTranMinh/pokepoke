package com.khangtm.pokepoke.core.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Elevated card; clickable when [onClick] is not null. */
@Composable
fun PokeCard(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        ElevatedCard(onClick = onClick, modifier = modifier, shape = shape, content = content)
    } else {
        ElevatedCard(modifier = modifier, shape = shape, content = content)
    }
}

/** Static label. */
@Composable
fun PokeChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = PokeSpacing.md, vertical = PokeSpacing.xs),
        )
    }
}
