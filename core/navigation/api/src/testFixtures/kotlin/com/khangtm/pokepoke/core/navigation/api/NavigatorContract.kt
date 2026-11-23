package com.khangtm.pokepoke.core.navigation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Simple [NavKey]s for tests. */
data class TestNavKey(val id: Int) : NavKey

/**
 * How a [Navigator] applies requests and goes back, with no interceptors. Extend it for the real implementation and
 * for [FakeNavigator], so both follow the same rules. [createNavigator] runs inside `runTest`; the contract advances
 * the scheduler after each request, so asynchronous implementations are fine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class NavigatorContract {

    abstract fun TestScope.createNavigator(initialStack: List<NavKey>): Navigator

    private fun key(id: Int) = TestNavKey(id)

    private fun TestScope.run(navigator: Navigator, vararg requests: NavRequest) {
        requests.forEach {
            navigator.execute(it)
            advanceUntilIdle()
        }
    }

    @Test
    fun `starts with the initial stack`() = runTest {
        assertEquals(listOf(key(1), key(2)), createNavigator(listOf(key(1), key(2))).backStack)
        assertTrue(createNavigator(emptyList()).backStack.isEmpty())
    }

    @Test
    fun `Push adds on top`() = runTest {
        val navigator = createNavigator(listOf(key(1)))
        run(navigator, NavRequest.Push(key(2)), NavRequest.Push(key(3)))
        assertEquals(listOf(key(1), key(2), key(3)), navigator.backStack)
    }

    @Test
    fun `Push on an empty stack adds the key`() = runTest {
        val navigator = createNavigator(emptyList())
        run(navigator, NavRequest.Push(key(1)))
        assertEquals(listOf(key(1)), navigator.backStack)
    }

    @Test
    fun `ReplaceTop replaces only the top key`() = runTest {
        val navigator = createNavigator(listOf(key(1), key(2)))
        run(navigator, NavRequest.ReplaceTop(key(9)))
        assertEquals(listOf(key(1), key(9)), navigator.backStack)
    }

    @Test
    fun `ReplaceTop on an empty stack behaves like Push`() = runTest {
        val navigator = createNavigator(emptyList())
        run(navigator, NavRequest.ReplaceTop(key(5)))
        assertEquals(listOf(key(5)), navigator.backStack)
    }

    @Test
    fun `ResetTo replaces the whole stack`() = runTest {
        val navigator = createNavigator(listOf(key(1), key(2), key(3)))
        run(navigator, NavRequest.ResetTo(listOf(key(7), key(8))))
        assertEquals(listOf(key(7), key(8)), navigator.backStack)
    }

    @Test
    fun `ResetTo works on an empty stack`() = runTest {
        val navigator = createNavigator(emptyList())
        run(navigator, NavRequest.ResetTo(listOf(key(1))))
        assertEquals(listOf(key(1)), navigator.backStack)
    }

    @Test
    fun `requests are applied in the order they were made`() = runTest {
        val navigator = createNavigator(emptyList())
        navigator.execute(NavRequest.ResetTo(listOf(key(1))))
        navigator.execute(NavRequest.Push(key(2)))
        navigator.execute(NavRequest.ReplaceTop(key(3)))
        navigator.execute(NavRequest.Push(key(4)))
        advanceUntilIdle()
        assertEquals(listOf(key(1), key(3), key(4)), navigator.backStack)
    }

    @Test
    fun `goBack pops the top key while more than one is left`() = runTest {
        val navigator = createNavigator(listOf(key(1), key(2), key(3)))
        assertTrue(navigator.goBack())
        assertEquals(listOf(key(1), key(2)), navigator.backStack)
        assertTrue(navigator.goBack())
        assertEquals(listOf(key(1)), navigator.backStack)
    }

    @Test
    fun `goBack returns false and keeps the last key`() = runTest {
        val navigator = createNavigator(listOf(key(1)))
        assertFalse(navigator.goBack())
        assertEquals(listOf(key(1)), navigator.backStack)
    }

    @Test
    fun `goBack on an empty stack returns false`() = runTest {
        val navigator = createNavigator(emptyList())
        assertFalse(navigator.goBack())
        assertTrue(navigator.backStack.isEmpty())
    }
}
