package com.khangtm.pokepoke.core.navigation

import androidx.navigation3.runtime.NavKey
import com.khangtm.pokepoke.core.testing.MainDispatcherRule
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A scope on the test scheduler. Not `backgroundScope`: `advanceUntilIdle()` only runs background tasks while
 * foreground work remains, so a request without suspension would never be processed.
 */
private fun TestScope.foregroundScope() = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

/** Applies the interceptor-free rules shared with the fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorImplContractTest : NavigatorContract() {
    override fun TestScope.createNavigator(initialStack: List<NavKey>): Navigator {
        val navigator = NavigatorImpl(emptySet(), foregroundScope())
        if (initialStack.isNotEmpty()) {
            navigator.execute(NavRequest.ResetTo(initialStack))
            advanceUntilIdle()
        }
        return navigator
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorImplTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun key(id: Int) = TestNavKey(id)

    private class RecordingInterceptor(
        override val priority: Int,
        private val calls: MutableList<String>,
        private val name: String = "p$priority",
        private val decide: (NavRequest, List<NavKey>) -> InterceptResult = { _, _ -> InterceptResult.Proceed },
    ) : NavigationInterceptor {
        val seenRequests = mutableListOf<NavRequest>()
        val seenStacks = mutableListOf<List<NavKey>>()

        override suspend fun intercept(request: NavRequest, currentStack: List<NavKey>): InterceptResult {
            calls += name
            seenRequests += request
            seenStacks += currentStack
            return decide(request, currentStack)
        }
    }

    private fun TestScope.navigator(vararg interceptors: NavigationInterceptor) =
        NavigatorImpl(interceptors.toSet(), foregroundScope())

    // ---- ordering and redirects ----

    @Test
    fun `interceptors run by ascending priority regardless of set order`() = runTest {
        val calls = mutableListOf<String>()
        val navigator = navigator(
            RecordingInterceptor(100, calls),
            RecordingInterceptor(0, calls),
            RecordingInterceptor(50, calls),
        )

        navigator.execute(NavRequest.Push(key(1)))
        advanceUntilIdle()

        assertEquals(listOf("p0", "p50", "p100"), calls)
        assertEquals(listOf(key(1)), navigator.backStack)
    }

    @Test
    fun `the first Redirect wins and later interceptors are not asked`() = runTest {
        val calls = mutableListOf<String>()
        val first = RecordingInterceptor(0, calls) { _, _ -> InterceptResult.Redirect(NavRequest.Push(key(10))) }
        val second = RecordingInterceptor(100, calls) { _, _ -> InterceptResult.Redirect(NavRequest.Push(key(20))) }
        val navigator = navigator(second, first)

        navigator.execute(NavRequest.Push(key(1)))
        advanceUntilIdle()

        assertEquals(listOf("p0"), calls)
        assertEquals(listOf(key(10)), navigator.backStack)
    }

    @Test
    fun `a redirected request is not intercepted again`() = runTest {
        val calls = mutableListOf<String>()
        // Would redirect forever if the redirect were intercepted again.
        val looping = RecordingInterceptor(0, calls) { _, _ -> InterceptResult.Redirect(NavRequest.Push(key(99))) }
        val navigator = navigator(looping)

        navigator.execute(NavRequest.Push(key(1)))
        advanceUntilIdle()

        assertEquals(1, looping.seenRequests.size)
        assertEquals(listOf(key(99)), navigator.backStack)
    }

    @Test
    fun `a Redirect can change the kind of request`() = runTest {
        val interceptor = RecordingInterceptor(0, mutableListOf()) { request, _ ->
            if (request is NavRequest.Push) InterceptResult.Redirect(NavRequest.ReplaceTop(key(5))) else InterceptResult.Proceed
        }
        val navigator = navigator(interceptor)
        navigator.execute(NavRequest.ResetTo(listOf(key(1), key(2))))
        advanceUntilIdle()

        navigator.execute(NavRequest.Push(key(3)))
        advanceUntilIdle()

        assertEquals(listOf(key(1), key(5)), navigator.backStack)
    }

    @Test
    fun `when every interceptor proceeds the original request is applied`() = runTest {
        val calls = mutableListOf<String>()
        val navigator = navigator(RecordingInterceptor(0, calls), RecordingInterceptor(1, calls))

        navigator.execute(NavRequest.ResetTo(listOf(key(1), key(2))))
        advanceUntilIdle()

        assertEquals(listOf("p0", "p1"), calls)
        assertEquals(listOf(key(1), key(2)), navigator.backStack)
    }

    @Test
    fun `interceptors receive the original request and the stack before it is applied`() = runTest {
        val interceptor = RecordingInterceptor(0, mutableListOf())
        val navigator = navigator(interceptor)
        navigator.execute(NavRequest.ResetTo(listOf(key(1))))
        advanceUntilIdle()

        navigator.execute(NavRequest.Push(key(2)))
        advanceUntilIdle()

        assertEquals(listOf<NavRequest>(NavRequest.ResetTo(listOf(key(1))), NavRequest.Push(key(2))), interceptor.seenRequests)
        assertEquals(listOf(emptyList(), listOf(key(1))), interceptor.seenStacks)
    }

    @Test
    fun `goBack does not run interceptors`() = runTest {
        val interceptor = RecordingInterceptor(0, mutableListOf())
        val navigator = navigator(interceptor)
        navigator.execute(NavRequest.ResetTo(listOf(key(1), key(2))))
        advanceUntilIdle()
        val before = interceptor.seenRequests.size

        assertTrue(navigator.goBack())

        assertEquals(before, interceptor.seenRequests.size)
        assertEquals(listOf(key(1)), navigator.backStack)
    }

    // ---- sequential processing ----

    @Test
    fun `requests are processed one at a time even when an interceptor suspends`() = runTest {
        val events = mutableListOf<String>()
        val slow = object : NavigationInterceptor {
            override val priority = 0
            override suspend fun intercept(request: NavRequest, currentStack: List<NavKey>): InterceptResult {
                val id = ((request as NavRequest.Push).key as TestNavKey).id
                events += "start$id"
                delay(if (id == 1) 1_000 else 10)
                events += "end$id"
                return InterceptResult.Proceed
            }
        }
        val navigator = navigator(slow)

        navigator.execute(NavRequest.Push(key(1)))
        navigator.execute(NavRequest.Push(key(2)))
        runCurrent()
        assertEquals(listOf("start1"), events)

        advanceTimeBy(1_001)
        advanceUntilIdle()

        assertEquals(listOf("start1", "end1", "start2", "end2"), events)
        assertEquals(listOf(key(1), key(2)), navigator.backStack)
    }

    @Test
    fun `the second request sees the stack left by the first`() = runTest {
        val interceptor = RecordingInterceptor(0, mutableListOf())
        val navigator = navigator(interceptor)

        navigator.execute(NavRequest.Push(key(1)))
        navigator.execute(NavRequest.Push(key(2)))
        navigator.execute(NavRequest.Push(key(3)))
        advanceUntilIdle()

        assertEquals(listOf(emptyList(), listOf(key(1)), listOf(key(1), key(2))), interceptor.seenStacks)
    }

    // ---- Hilt constructor and lifecycle ----

    private class FakeLifecycle : ActivityRetainedLifecycle {
        private val listeners = mutableListOf<RetainedLifecycle.OnClearedListener>()
        override fun addOnClearedListener(listener: RetainedLifecycle.OnClearedListener) {
            listeners += listener
        }

        override fun removeOnClearedListener(listener: RetainedLifecycle.OnClearedListener) {
            listeners -= listener
        }

        fun clear() = listeners.toList().forEach { it.onCleared() }
    }

    @Test
    fun `the injected navigator works on Main and stops when the retained component is cleared`() {
        val lifecycle = FakeLifecycle()
        val navigator = NavigatorImpl(emptySet(), lifecycle)

        navigator.execute(NavRequest.Push(key(1)))
        mainRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(key(1)), navigator.backStack)

        lifecycle.clear()
        navigator.execute(NavRequest.Push(key(2)))
        mainRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("scope cancelled: no more requests are processed", listOf(key(1)), navigator.backStack)
    }

    @Test
    fun `backStack is the same live list across calls`() = runTest {
        val navigator = navigator()
        val list = navigator.backStack
        navigator.execute(NavRequest.Push(key(1)))
        advanceUntilIdle()
        assertEquals(listOf(key(1)), list)
        assertTrue(navigator.backStack === list)
    }
}
