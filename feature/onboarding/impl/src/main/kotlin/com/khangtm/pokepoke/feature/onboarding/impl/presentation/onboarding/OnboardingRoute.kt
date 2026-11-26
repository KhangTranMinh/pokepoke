package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khangtm.pokepoke.core.navigation.NavRequest

@Composable
internal fun OnboardingRoute(
    pending: NavRequest,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel<OnboardingViewModel, OnboardingViewModel.Factory>(
        creationCallback = { it.create(pending) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}
