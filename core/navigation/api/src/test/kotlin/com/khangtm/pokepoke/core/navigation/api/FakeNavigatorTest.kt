package com.khangtm.pokepoke.core.navigation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

/** The fake must follow the same contract as the real navigator. */
class FakeNavigatorContractTest : NavigatorContract() {
    override fun TestScope.createNavigator(initialStack: List<NavKey>): Navigator = FakeNavigator(initialStack)
}

class FakeNavigatorTest {

    private val a = TestNavKey(1)
    private val b = TestNavKey(2)

    @Test
    fun `records every request in order`() {
        val navigator = FakeNavigator()
        val requests = listOf(NavRequest.ResetTo(listOf(a)), NavRequest.Push(b), NavRequest.ReplaceTop(a))
        requests.forEach(navigator::execute)
        assertEquals(requests, navigator.requests)
    }

    @Test
    fun `counts goBack calls including the ones that cannot pop`() {
        val navigator = FakeNavigator(listOf(a))
        navigator.goBack()
        navigator.goBack()
        assertEquals(2, navigator.backCount)
    }

    @Test
    fun `starts with no requests and no back presses`() {
        val navigator = FakeNavigator(listOf(a, b))
        assertEquals(emptyList<NavRequest>(), navigator.requests)
        assertEquals(0, navigator.backCount)
    }
}
