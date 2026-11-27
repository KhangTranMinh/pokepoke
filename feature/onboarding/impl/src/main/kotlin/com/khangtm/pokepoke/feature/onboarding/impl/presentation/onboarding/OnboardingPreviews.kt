package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.khangtm.pokepoke.ui.designsystem.PokeTheme

/** Sample states shared by the previews and the screenshot tests. */
internal object OnboardingSamples {
    val firstPage = OnboardingUiState(page = 0)
    val secondPage = OnboardingUiState(page = 1)
    val lastPage = OnboardingUiState(page = 2)
    val finishing = OnboardingUiState(page = 2, isFinishing = true)
}

internal class OnboardingStateProvider : PreviewParameterProvider<OnboardingUiState> {
    override val values = sequenceOf(
        OnboardingSamples.firstPage,
        OnboardingSamples.secondPage,
        OnboardingSamples.lastPage,
        OnboardingSamples.finishing,
    )
}

@PreviewLightDark
@Composable
private fun OnboardingScreenPreview(@PreviewParameter(OnboardingStateProvider::class) state: OnboardingUiState) {
    PokeTheme { OnboardingScreen(state = state, onAction = {}) }
}
