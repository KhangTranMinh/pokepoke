package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

internal sealed interface OnboardingUiAction {
    data class PageChanged(val page: Int) : OnboardingUiAction

    /** Goes to the next page; on the last page it behaves like [Finish]. */
    data object Next : OnboardingUiAction

    /** Same as [Finish]. */
    data object Skip : OnboardingUiAction

    data object Finish : OnboardingUiAction
}
