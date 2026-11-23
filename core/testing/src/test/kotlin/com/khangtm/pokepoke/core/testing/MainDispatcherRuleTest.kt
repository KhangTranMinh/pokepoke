package com.khangtm.pokepoke.core.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {

    private fun runWith(rule: MainDispatcherRule, body: () -> Unit) {
        rule.apply(object : Statement() {
            override fun evaluate() = body()
        }, Description.EMPTY).evaluate()
    }

    @Test
    fun `coroutine on Main runs only after the scheduler advances`() {
        val rule = MainDispatcherRule()
        var ran = false
        runWith(rule) {
            CoroutineScope(Dispatchers.Main).launch { ran = true }
            assertFalse(ran)
            rule.testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(ran)
        }
    }

    @Test
    fun `uses the dispatcher passed to the rule`() {
        val custom = StandardTestDispatcher()
        val rule = MainDispatcherRule(custom)
        assertSame(custom, rule.testDispatcher)
        var ran = false
        runWith(rule) {
            CoroutineScope(Dispatchers.Main).launch { ran = true }
            custom.scheduler.advanceUntilIdle()
        }
        assertTrue(ran)
    }

    @Test
    fun `rule restores Main after the test so a later rule can install its own`() {
        val first = MainDispatcherRule()
        runWith(first) { }
        val second = MainDispatcherRule()
        var order = emptyList<Int>()
        runWith(second) {
            CoroutineScope(Dispatchers.Main).launch { order = order + 1 }
            second.testDispatcher.scheduler.advanceUntilIdle()
        }
        assertEquals(listOf(1), order)
    }
}
