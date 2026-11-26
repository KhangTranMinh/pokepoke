package com.khangtm.pokepoke.feature.onboarding.impl.domain

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.storage.api.FakeKeyValueStoreFactory
import com.khangtm.pokepoke.feature.onboarding.impl.data.OnboardingRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUseCasesTest {

    private val factory = FakeKeyValueStoreFactory()
    private val store = factory.create(OnboardingRepositoryImpl.STORE_NAME)
    private val repository = OnboardingRepositoryImpl(factory)
    private val isCompleted = IsOnboardingCompletedUseCaseImpl(repository)
    private val complete = CompleteOnboardingUseCaseImpl(repository)

    @Test
    fun `not completed on first launch`() = runTest {
        assertEquals(AppResult.Success(false), isCompleted())
    }

    @Test
    fun `completed after completeOnboarding`() = runTest {
        assertEquals(AppResult.Success(Unit), complete())

        assertEquals(AppResult.Success(true), isCompleted())
    }

    @Test
    fun `reading failure becomes Unknown`() = runTest {
        val cause = IllegalStateException("disk")
        store.failWith = cause

        assertEquals(AppResult.Failure(AppError.Unknown(cause)), isCompleted())
    }

    @Test
    fun `writing failure becomes Unknown and nothing is stored`() = runTest {
        val cause = IllegalStateException("disk full")
        store.failWith = cause

        assertEquals(AppResult.Failure(AppError.Unknown(cause)), complete())

        store.failWith = null
        assertEquals(AppResult.Success(false), isCompleted())
    }

    @Test
    fun `failure results are failures`() = runTest {
        store.failWith = IllegalStateException("disk")

        assertTrue(isCompleted() is AppResult.Failure)
        assertTrue(complete() is AppResult.Failure)
    }
}
