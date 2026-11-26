package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.navigation.FakeNavigator
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.TestNavKey
import com.khangtm.pokepoke.core.testing.MainDispatcherRule
import com.khangtm.pokepoke.feature.onboarding.impl.domain.CompleteOnboardingUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val pending = NavRequest.ResetTo(listOf(TestNavKey(1), TestNavKey(25)))
    private val navigator = FakeNavigator()
    private var completeCalls = 0
    private var completeResult: AppResult<Unit> = AppResult.Success(Unit)
    private val complete = CompleteOnboardingUseCase {
        completeCalls++
        completeResult
    }

    private fun viewModel() = OnboardingViewModel(complete, navigator, pending)

    @Test
    fun `starts on the first page`() {
        val state = viewModel().uiState.value

        assertEquals(0, state.page)
        assertEquals(3, state.pageCount)
        assertFalse(state.isLastPage)
        assertFalse(state.isFinishing)
    }

    @Test
    fun `next moves to the following page`() {
        val viewModel = viewModel()

        viewModel.onAction(OnboardingUiAction.Next)
        assertEquals(1, viewModel.uiState.value.page)
        viewModel.onAction(OnboardingUiAction.Next)

        assertEquals(2, viewModel.uiState.value.page)
        assertTrue(viewModel.uiState.value.isLastPage)
        assertEquals(0, completeCalls)
    }

    @Test
    fun `page changed by swiping updates the page and is kept in range`() {
        val viewModel = viewModel()

        viewModel.onAction(OnboardingUiAction.PageChanged(2))
        assertEquals(2, viewModel.uiState.value.page)
        viewModel.onAction(OnboardingUiAction.PageChanged(9))
        assertEquals(2, viewModel.uiState.value.page)
        viewModel.onAction(OnboardingUiAction.PageChanged(-4))
        assertEquals(0, viewModel.uiState.value.page)
    }

    @Test
    fun `next on the last page finishes`() = runTest(mainDispatcher.testDispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(OnboardingUiAction.PageChanged(2))

        viewModel.onAction(OnboardingUiAction.Next)
        assertTrue(viewModel.uiState.value.isFinishing)
        advanceUntilIdle()

        assertEquals(1, completeCalls)
        assertEquals(listOf<NavRequest>(pending), navigator.requests)
    }

    @Test
    fun `skip completes onboarding then runs the pending request`() = runTest(mainDispatcher.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(OnboardingUiAction.Skip)
        assertTrue(viewModel.uiState.value.isFinishing)
        assertTrue(navigator.requests.isEmpty())
        advanceUntilIdle()

        assertEquals(1, completeCalls)
        assertEquals(listOf<NavRequest>(pending), navigator.requests)
    }

    @Test
    fun `finish completes onboarding then runs the pending request`() = runTest(mainDispatcher.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(OnboardingUiAction.Finish)
        advanceUntilIdle()

        assertEquals(1, completeCalls)
        assertEquals(listOf<NavRequest>(pending), navigator.requests)
    }

    @Test
    fun `finish still continues when saving fails`() = runTest(mainDispatcher.testDispatcher) {
        completeResult = AppResult.Failure(AppError.Unknown(IllegalStateException("disk full")))
        val viewModel = viewModel()

        viewModel.onAction(OnboardingUiAction.Finish)
        advanceUntilIdle()

        assertEquals(1, completeCalls)
        assertEquals(listOf<NavRequest>(pending), navigator.requests)
    }

    @Test
    fun `finishing twice only continues once`() = runTest(mainDispatcher.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(OnboardingUiAction.Finish)
        viewModel.onAction(OnboardingUiAction.Skip)
        viewModel.onAction(OnboardingUiAction.Next)
        advanceUntilIdle()

        assertEquals(1, completeCalls)
        assertEquals(1, navigator.requests.size)
    }
}
