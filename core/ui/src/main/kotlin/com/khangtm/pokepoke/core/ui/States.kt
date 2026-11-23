package com.khangtm.pokepoke.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.designsystem.PokeIcons
import com.khangtm.pokepoke.core.designsystem.PokeSecondaryButton
import com.khangtm.pokepoke.core.designsystem.PokeSizes
import com.khangtm.pokepoke.core.designsystem.PokeSpacing
import com.khangtm.pokepoke.core.designsystem.PokeTextButton

private val StateIconSize = 48.dp
private val FooterProgressSize = 24.dp

/** Centered spinner that fills the available area. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.ui_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

/** Icon, title and message chosen by [error], plus a Retry button. */
@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = PokeSizes.maxContentWidth).padding(PokeSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PokeSpacing.sm),
        ) {
            Icon(
                imageVector = if (error == AppError.NoConnection) PokeIcons.CloudOff else PokeIcons.Pokeball,
                contentDescription = null,
                modifier = Modifier.size(StateIconSize),
                tint = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(error.titleRes()),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(error.messageRes(), *error.messageArgs().toTypedArray()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(PokeSpacing.sm))
            PokeSecondaryButton(text = stringResource(R.string.ui_retry), onClick = onRetry)
        }
    }
}

/** For empty lists, "login required", placeholder panes. The action button needs both [actionText] and [onAction]. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = PokeSizes.maxContentWidth).padding(PokeSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PokeSpacing.sm),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(StateIconSize),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(PokeSpacing.sm))
                PokeSecondaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

/** Full-width strip shown when the data on screen comes from the cache because there is no network. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = PokeSpacing.lg, vertical = PokeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PokeSpacing.sm),
    ) {
        Icon(
            imageVector = PokeIcons.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = stringResource(R.string.ui_offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

sealed interface LoadMoreState {
    data object Idle : LoadMoreState
    data object Loading : LoadMoreState
    data class Error(val error: AppError) : LoadMoreState
    data object EndReached : LoadMoreState
}

/** Footer of a paged list. Shows nothing for [LoadMoreState.Idle] and [LoadMoreState.EndReached]. */
@Composable
fun LoadMoreFooter(
    state: LoadMoreState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        LoadMoreState.Idle, LoadMoreState.EndReached -> Unit
        LoadMoreState.Loading -> {
            val description = stringResource(R.string.ui_loading)
            Box(
                modifier = modifier.fillMaxWidth().padding(PokeSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(FooterProgressSize).semantics { contentDescription = description },
                    strokeWidth = 2.dp,
                )
            }
        }

        is LoadMoreState.Error -> Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = PokeSpacing.lg, vertical = PokeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.ui_load_more_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PokeTextButton(text = stringResource(R.string.ui_retry), onClick = onRetry)
        }
    }
}
