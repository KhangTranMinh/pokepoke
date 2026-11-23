package com.khangtm.pokepoke.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val ProgressSize = 20.dp
private val ProgressStroke = 2.dp

/** Full-width filled button. While [loading] a spinner replaces the text and clicks are ignored. */
@Composable
fun PokePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier.fillMaxWidth().height(PokeSizes.buttonHeight),
        enabled = enabled,
    ) {
        if (loading) {
            val description = stringResource(R.string.designsystem_loading)
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize).semantics { contentDescription = description },
                strokeWidth = ProgressStroke,
                color = LocalContentColor.current,
            )
        } else {
            Text(text)
        }
    }
}

/** Full-width outlined button. */
@Composable
fun PokeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = { if (!loading) onClick() },
        modifier = modifier.fillMaxWidth().height(PokeSizes.buttonHeight),
        enabled = enabled,
    ) {
        if (loading) {
            val description = stringResource(R.string.designsystem_loading)
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize).semantics { contentDescription = description },
                strokeWidth = ProgressStroke,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun PokeTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text) }
}
