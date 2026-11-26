package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

internal data class OnboardingUiState(
    val page: Int = 0,
    val pageCount: Int = 3,
    val isFinishing: Boolean = false,
) {
    val isLastPage: Boolean get() = page == pageCount - 1
}
