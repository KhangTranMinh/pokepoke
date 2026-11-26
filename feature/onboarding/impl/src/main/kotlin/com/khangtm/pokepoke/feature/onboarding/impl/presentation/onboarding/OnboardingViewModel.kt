package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.Navigator
import com.khangtm.pokepoke.feature.onboarding.impl.domain.CompleteOnboardingUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OnboardingViewModel.Factory::class)
internal class OnboardingViewModel @AssistedInject constructor(
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val navigator: Navigator,
    @Assisted private val pending: NavRequest,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(pending: NavRequest): OnboardingViewModel
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onAction(action: OnboardingUiAction) {
        when (action) {
            is OnboardingUiAction.PageChanged -> _uiState.update {
                it.copy(page = action.page.coerceIn(0, it.pageCount - 1))
            }
            OnboardingUiAction.Next ->
                if (_uiState.value.isLastPage) finish() else _uiState.update { it.copy(page = it.page + 1) }
            OnboardingUiAction.Skip, OnboardingUiAction.Finish -> finish()
        }
    }

    private fun finish() {
        if (_uiState.value.isFinishing) return
        _uiState.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            // Success or Failure, the user continues; on Failure onboarding is shown again next launch (contracts.md §9).
            completeOnboarding()
            navigator.execute(pending)
        }
    }
}
