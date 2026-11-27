package com.khangtm.pokepoke.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.ui.designsystem.PokeIcons
import com.khangtm.pokepoke.ui.designsystem.PokeSizes
import com.khangtm.pokepoke.ui.designsystem.PokeSpacing
import com.khangtm.pokepoke.ui.designsystem.PokeTheme

// Sample content shared by the @Preview functions below and by the screenshot tests.
// Texts here are preview data only, not app UI.

@Composable
internal fun UiSampleSurface(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(PokeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PokeSpacing.md),
        ) { content() }
    }
}

/** Makes Coil render a fixed state instead of loading, so previews and screenshots are deterministic. */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun WithImageState(state: PreviewImageState, content: @Composable () -> Unit) {
    val handler = when (state) {
        PreviewImageState.Loading -> AsyncImagePreviewHandler { _, _ -> AsyncImagePainter.State.Loading(painter = null) }
        PreviewImageState.Failed -> AsyncImagePreviewHandler { _, request ->
            AsyncImagePainter.State.Error(
                painter = null,
                result = ErrorResult(image = null, request = request, throwable = IllegalStateException("preview")),
            )
        }
        PreviewImageState.Loaded -> AsyncImagePreviewHandler { ColorImage(color = 0xFF3D5A80.toInt()) }
    }
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalAsyncImagePreviewHandler provides handler,
        content = content,
    )
}

internal enum class PreviewImageState { Loading, Failed, Loaded }

@Composable
private fun SampleImage(state: PreviewImageState, url: String?) {
    WithImageState(state) {
        RemoteImage(url = url, contentDescription = null, modifier = Modifier.size(PokeSizes.cardImage))
    }
}

@Composable
internal fun RemoteImageSample() {
    UiSampleSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(PokeSpacing.md)) {
            SampleImage(PreviewImageState.Loaded, url = null)
            SampleImage(PreviewImageState.Loading, url = "https://example.com/a.png")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PokeSpacing.md)) {
            SampleImage(PreviewImageState.Failed, url = "https://example.com/b.png")
            SampleImage(PreviewImageState.Loaded, url = "https://example.com/c.png")
        }
    }
}

@Composable
internal fun SizedSample(height: Dp, content: @Composable () -> Unit) {
    UiSampleSurface {
        Column(Modifier.height(height)) { content() }
    }
}

@Composable
internal fun LoadingStateSample() = SizedSample(160.dp) { LoadingState() }

@Composable
internal fun ErrorStateSample(error: AppError) = SizedSample(360.dp) { ErrorState(error = error, onRetry = {}) }

@Composable
internal fun EmptyStateSample() {
    SizedSample(420.dp) {
        EmptyState(
            title = "Nothing here yet",
            message = "Add a Pokémon to see it in this list.",
            icon = PokeIcons.FavouriteSelected,
            actionText = "Browse",
            onAction = {},
        )
    }
}

@Composable
internal fun EmptyStateMinimalSample() = SizedSample(160.dp) { EmptyState(title = "Select a Pokémon") }

@Composable
internal fun OfflineBannerSample() = UiSampleSurface { OfflineBanner() }

@Composable
internal fun LoadMoreFooterSample() {
    UiSampleSurface {
        LoadMoreFooter(state = LoadMoreState.Loading, onRetry = {})
        LoadMoreFooter(state = LoadMoreState.Error(AppError.NoConnection), onRetry = {})
    }
}

@Composable
internal fun MediaCardSample() {
    UiSampleSurface {
        WithImageState(PreviewImageState.Loaded) {
            Row(horizontalArrangement = Arrangement.spacedBy(PokeSpacing.md)) {
                MediaCard(
                    title = "Bulbasaur",
                    overline = "#001",
                    imageUrl = "https://example.com/1.png",
                    onClick = {},
                    modifier = Modifier.size(width = 168.dp, height = 220.dp),
                )
                MediaCard(
                    title = "Pikachu",
                    overline = "#025",
                    imageUrl = null,
                    onClick = {},
                    modifier = Modifier.size(width = 168.dp, height = 220.dp),
                    action = {
                        IconButton(onClick = {}) { Icon(PokeIcons.FavouriteSelected, contentDescription = null) }
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RemoteImagePreview() = PokeTheme { RemoteImageSample() }

@PreviewLightDark
@Composable
private fun LoadingStatePreview() = PokeTheme { LoadingStateSample() }

@PreviewLightDark
@Composable
private fun ErrorStatePreview() = PokeTheme { ErrorStateSample(AppError.NoConnection) }

@PreviewLightDark
@Composable
private fun EmptyStatePreview() = PokeTheme { EmptyStateSample() }

@PreviewLightDark
@Composable
private fun OfflineBannerPreview() = PokeTheme { OfflineBannerSample() }

@PreviewLightDark
@Composable
private fun LoadMoreFooterPreview() = PokeTheme { LoadMoreFooterSample() }

@PreviewLightDark
@Composable
private fun MediaCardPreview() = PokeTheme { MediaCardSample() }
