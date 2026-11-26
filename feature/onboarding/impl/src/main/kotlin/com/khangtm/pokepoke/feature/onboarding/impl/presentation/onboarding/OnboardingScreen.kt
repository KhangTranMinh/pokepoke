package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khangtm.pokepoke.core.designsystem.PokeIcons
import com.khangtm.pokepoke.core.designsystem.PokePrimaryButton
import com.khangtm.pokepoke.core.designsystem.PokeSizes
import com.khangtm.pokepoke.core.designsystem.PokeSpacing
import com.khangtm.pokepoke.core.designsystem.PokeTextButton
import com.khangtm.pokepoke.translation.R

private val IllustrationCircleSize = 200.dp
private val IllustrationIconSize = 120.dp
private val DotSize = 8.dp
private val SelectedDotWidth = 24.dp

private enum class OnboardingPage(@StringRes val title: Int, @StringRes val body: Int) {
    Discover(R.string.onboarding_page1_title, R.string.onboarding_page1_body),
    Search(R.string.onboarding_page2_title, R.string.onboarding_page2_body),
    Favourites(R.string.onboarding_page3_title, R.string.onboarding_page3_body),
}

@Composable
private fun OnboardingPage.icon(): ImageVector = when (this) {
    OnboardingPage.Discover -> PokeIcons.Pokeball
    OnboardingPage.Search -> PokeIcons.Search
    OnboardingPage.Favourites -> PokeIcons.FavouriteSelected
}

@Composable
internal fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = state.page, pageCount = { state.pageCount })

    // The pager is the source of truth while swiping; the state drives it for the Next button.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { onAction(OnboardingUiAction.PageChanged(it)) }
    }
    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) pagerState.animateScrollToPage(state.page)
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(PokeSizes.minTouchTarget).padding(horizontal = PokeSpacing.sm),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.isLastPage) {
                    PokeTextButton(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = { if (!state.isFinishing) onAction(OnboardingUiAction.Skip) },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                OnboardingPageContent(OnboardingPage.entries[index])
            }
            PageIndicator(count = state.pageCount, selected = state.page)
            PokePrimaryButton(
                text = stringResource(if (state.isLastPage) R.string.onboarding_get_started else R.string.onboarding_next),
                onClick = { onAction(OnboardingUiAction.Next) },
                loading = state.isFinishing,
                modifier = Modifier
                    .widthIn(max = PokeSizes.maxContentWidth)
                    .padding(horizontal = PokeSpacing.xl, vertical = PokeSpacing.xl),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = PokeSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(IllustrationCircleSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon(),
                contentDescription = null,
                modifier = Modifier.size(IllustrationIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(PokeSpacing.xxl))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = PokeSizes.maxContentWidth),
        )
        Spacer(Modifier.height(PokeSpacing.md))
        Text(
            text = stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = PokeSizes.maxContentWidth),
        )
    }
}

/** Decorative: the current page is already conveyed by the page content and the button label. */
@Composable
private fun PageIndicator(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(PokeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isSelected = index == selected
            val width by animateDpAsState(if (isSelected) SelectedDotWidth else DotSize, label = "dotWidth")
            val color by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(Modifier.size(width = width, height = DotSize).clip(CircleShape).background(color))
        }
    }
}
