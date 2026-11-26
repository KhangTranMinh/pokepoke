package com.khangtm.pokepoke.feature.onboarding.impl.navigation

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.navigation.InterceptResult
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.TestNavKey
import com.khangtm.pokepoke.feature.onboarding.api.OnboardingNavKey
import com.khangtm.pokepoke.feature.onboarding.impl.domain.IsOnboardingCompletedUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingInterceptorTest {

    private val request = NavRequest.ResetTo(listOf(TestNavKey(1), TestNavKey(2)))

    private fun interceptor(result: AppResult<Boolean>) = OnboardingInterceptor(IsOnboardingCompletedUseCase { result })

    @Test
    fun `runs before the auth interceptor`() {
        assertEquals(0, interceptor(AppResult.Success(true)).priority)
    }

    @Test
    fun `not seen yet redirects to onboarding carrying the request`() = runTest {
        val result = interceptor(AppResult.Success(false)).intercept(request, currentStack = emptyList())

        assertEquals(
            InterceptResult.Redirect(NavRequest.ResetTo(listOf(OnboardingNavKey(pending = request)))),
            result,
        )
    }

    @Test
    fun `already seen proceeds`() = runTest {
        val result = interceptor(AppResult.Success(true)).intercept(request, currentStack = emptyList())

        assertEquals(InterceptResult.Proceed, result)
    }

    @Test
    fun `a storage failure never blocks the app`() = runTest {
        val result = interceptor(AppResult.Failure(AppError.Unknown())).intercept(request, currentStack = emptyList())

        assertEquals(InterceptResult.Proceed, result)
    }

    @Test
    fun `a request that already holds the onboarding key proceeds without asking`() = runTest {
        val onboarding = OnboardingNavKey(pending = request)
        val interceptor = OnboardingInterceptor(IsOnboardingCompletedUseCase { error("must not be asked") })

        listOf(
            NavRequest.Push(onboarding),
            NavRequest.ReplaceTop(onboarding),
            NavRequest.ResetTo(listOf(TestNavKey(1), onboarding)),
        ).forEach {
            assertEquals(InterceptResult.Proceed, interceptor.intercept(it, currentStack = emptyList()))
        }
    }

    @Test
    fun `push and replace-top requests are redirected too`() = runTest {
        val interceptor = interceptor(AppResult.Success(false))

        listOf(NavRequest.Push(TestNavKey(3)), NavRequest.ReplaceTop(TestNavKey(3))).forEach {
            assertEquals(
                InterceptResult.Redirect(NavRequest.ResetTo(listOf(OnboardingNavKey(pending = it)))),
                interceptor.intercept(it, currentStack = listOf(TestNavKey(1))),
            )
        }
    }
}
