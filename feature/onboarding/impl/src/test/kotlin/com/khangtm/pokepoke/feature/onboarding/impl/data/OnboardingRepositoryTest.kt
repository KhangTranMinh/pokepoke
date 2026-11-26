package com.khangtm.pokepoke.feature.onboarding.impl.data

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.storage.api.FakeKeyValueStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryTest {

    private val factory = FakeKeyValueStoreFactory()
    private val store = factory.create(OnboardingRepositoryImpl.STORE_NAME)
    private val repository = OnboardingRepositoryImpl(factory)

    @Test
    fun `not completed before anything was written`() = runTest {
        assertEquals(DataResult.Success(false), repository.observeCompleted().first())
    }

    @Test
    fun `markCompleted stores the flag and observers see it`() = runTest {
        assertEquals(DataResult.Success(Unit), repository.markCompleted())

        assertEquals(DataResult.Success(true), repository.observeCompleted().first())
        assertEquals(true, store.observeBoolean(OnboardingRepositoryImpl.KEY_COMPLETED).first())
    }

    @Test
    fun `storage error while reading becomes Local Read`() = runTest {
        val cause = IllegalStateException("disk")
        store.failWith = cause

        val result = repository.observeCompleted().first()

        assertEquals(DataResult.Failure(DataError.Local.Read(cause)), result)
    }

    @Test
    fun `storage error while writing becomes Local Write`() = runTest {
        val cause = IllegalStateException("disk full")
        store.failWith = cause

        val result = repository.markCompleted()

        assertEquals(DataResult.Failure(DataError.Local.Write(cause)), result)
    }

    @Test
    fun `works again once the storage recovers`() = runTest {
        store.failWith = IllegalStateException("disk")
        assertTrue(repository.observeCompleted().first() is DataResult.Failure)

        store.failWith = null
        assertEquals(DataResult.Success(Unit), repository.markCompleted())
        assertEquals(DataResult.Success(true), repository.observeCompleted().first())
    }
}
