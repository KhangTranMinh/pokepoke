package com.khangtm.pokepoke.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.NavKey
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.navigation.InterceptResult
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.NavigationInterceptor
import com.khangtm.pokepoke.feature.onboarding.api.OnboardingNavKey
import com.khangtm.pokepoke.feature.onboarding.impl.domain.IsOnboardingCompletedUseCase
import javax.inject.Inject

/**
 * Sends the user to onboarding on the first launch, carrying the original request as `pending`.
 * A storage error never blocks the app: it proceeds as if onboarding was done.
 */
internal class OnboardingInterceptor @Inject constructor(
    private val isOnboardingCompleted: IsOnboardingCompletedUseCase,
) : NavigationInterceptor {

    override val priority: Int = PRIORITY

    override suspend fun intercept(request: NavRequest, currentStack: List<NavKey>): InterceptResult {
        if (request.keys().any { it is OnboardingNavKey }) return InterceptResult.Proceed
        return when (val completed = isOnboardingCompleted()) {
            is AppResult.Success ->
                if (completed.value) {
                    InterceptResult.Proceed
                } else {
                    InterceptResult.Redirect(NavRequest.ResetTo(listOf(OnboardingNavKey(pending = request))))
                }
            is AppResult.Failure -> InterceptResult.Proceed
        }
    }

    private fun NavRequest.keys(): List<NavKey> = when (this) {
        is NavRequest.Push -> listOf(key)
        is NavRequest.ReplaceTop -> listOf(key)
        is NavRequest.ResetTo -> keys
    }

    internal companion object {
        const val PRIORITY = 0
    }
}
