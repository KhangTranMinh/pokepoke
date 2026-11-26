package com.khangtm.pokepoke.feature.onboarding.impl.domain

import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.toAppResult
import com.khangtm.pokepoke.feature.onboarding.impl.data.OnboardingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Whether the user already went through (or skipped) onboarding. */
internal fun interface IsOnboardingCompletedUseCase {
    suspend operator fun invoke(): AppResult<Boolean>
}

/** Remembers that onboarding is done, so it is not shown again. */
internal fun interface CompleteOnboardingUseCase {
    suspend operator fun invoke(): AppResult<Unit>
}

internal class IsOnboardingCompletedUseCaseImpl @Inject constructor(
    private val repository: OnboardingRepository,
) : IsOnboardingCompletedUseCase {
    override suspend fun invoke(): AppResult<Boolean> = repository.observeCompleted().first().toAppResult()
}

internal class CompleteOnboardingUseCaseImpl @Inject constructor(
    private val repository: OnboardingRepository,
) : CompleteOnboardingUseCase {
    override suspend fun invoke(): AppResult<Unit> = repository.markCompleted().toAppResult()
}
