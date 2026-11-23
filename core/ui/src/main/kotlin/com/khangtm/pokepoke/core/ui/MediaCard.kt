package com.khangtm.pokepoke.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.khangtm.pokepoke.core.designsystem.PokeCard
import com.khangtm.pokepoke.core.designsystem.PokeSizes
import com.khangtm.pokepoke.core.designsystem.PokeSpacing

/** Card with a square image on top, an overline and a title; [action] sits in the top-right corner. */
@Composable
fun MediaCard(
    title: String,
    overline: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    PokeCard(onClick = onClick, modifier = modifier) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(PokeSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RemoteImage(
                    url = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(PokeSizes.cardImage),
                )
                Spacer(Modifier.height(PokeSpacing.sm))
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null) {
                Box(Modifier.align(Alignment.TopEnd)) { action() }
            }
        }
    }
}
