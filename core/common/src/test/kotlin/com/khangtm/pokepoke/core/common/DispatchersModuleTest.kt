package com.khangtm.pokepoke.core.common

import com.khangtm.pokepoke.core.common.di.CommonModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.isActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class DispatchersModuleTest {

    @Test
    fun `provides the real IO and Default dispatchers`() {
        assertSame(Dispatchers.IO, CommonModule.provideIoDispatcher())
        assertSame(Dispatchers.Default, CommonModule.provideDefaultDispatcher())
    }

    @Test
    fun `application scope uses the given dispatcher and survives a failing child`() {
        val dispatcher = StandardTestDispatcher()
        val scope = CommonModule.provideApplicationScope(dispatcher)
        assertEquals(dispatcher, scope.coroutineContext[ContinuationInterceptor])

        var secondRan = false
        val failing = scope.async<Unit> { throw IllegalStateException("child fails") }
        scope.launch { secondRan = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(failing.isCancelled)
        assertTrue(scope.isActive)
        assertTrue(secondRan)
    }
}
