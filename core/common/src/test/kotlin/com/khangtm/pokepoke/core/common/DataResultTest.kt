package com.khangtm.pokepoke.core.common

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DataResultTest {

    private suspend fun assertCancelled(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected CancellationException")
        } catch (expected: CancellationException) {
            // rethrown as required
        }
    }

    private val mapToUnexpected: (Throwable) -> DataError = { DataError.Unexpected(it) }

    @Test
    fun `dataResultOf returns Success with the block value`() = runTest {
        assertEquals(DataResult.Success(42), dataResultOf(mapToUnexpected) { 42 })
    }

    @Test
    fun `dataResultOf maps a thrown exception with mapError`() = runTest {
        val boom = IOException("boom")
        val result = dataResultOf({ DataError.Local.Read(it) }) { throw boom }
        assertEquals(DataResult.Failure(DataError.Local.Read(boom)), result)
    }

    @Test
    fun `dataResultOf catches Error as well as Exception`() = runTest {
        val result = dataResultOf(mapToUnexpected) { throw StackOverflowError() }
        assertTrue(result is DataResult.Failure)
    }

    @Test
    fun `dataResultOf rethrows CancellationException`() = runTest {
        assertCancelled { dataResultOf(mapToUnexpected) { throw CancellationException("cancelled") } }
    }

    @Test
    fun `map transforms Success and keeps Failure`() {
        assertEquals(DataResult.Success(2), DataResult.Success(1).map { it + 1 })
        val failure: DataResult<Int> = DataResult.Failure(DataError.NotFound)
        assertSame(failure, failure.map { it + 1 })
    }

    @Test
    fun `flatMap chains Success and short circuits Failure`() = runTest {
        val ok: DataResult<Int> = DataResult.Success(1)
        assertEquals(DataResult.Success("1!"), ok.flatMap { DataResult.Success("$it!") })
        assertEquals(
            DataResult.Failure(DataError.NotFound),
            ok.flatMap<Int, String> { DataResult.Failure(DataError.NotFound) },
        )
        var called = false
        val failure: DataResult<Int> = DataResult.Failure(DataError.NotFound)
        failure.flatMap { called = true; DataResult.Success(it) }
        assertTrue(!called)
    }

    @Test
    fun `flatMap lambda may call suspend functions`() = runTest {
        suspend fun load(id: Int): DataResult<String> = DataResult.Success("item$id")
        assertEquals(DataResult.Success("item7"), DataResult.Success(7).flatMap { load(it) })
    }

    @Test
    fun `asDataResult wraps every value in Success`() = runTest {
        flow { emit(1); emit(2) }.asDataResult(mapToUnexpected).test {
            assertEquals(DataResult.Success(1), awaitItem())
            assertEquals(DataResult.Success(2), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `asDataResult emits Failure after an error then resubscribes and emits Success`() = runTest {
        var subscriptions = 0
        val upstream = flow {
            subscriptions++
            if (subscriptions < 3) throw IOException("fail $subscriptions")
            emit("data")
        }
        upstream.asDataResult({ DataError.Local.Read(it) }).test {
            assertTrue((awaitItem() as DataResult.Failure).error is DataError.Local.Read)
            assertEquals(0, testScheduler.currentTime)
            assertTrue(awaitItem() is DataResult.Failure)
            assertEquals(1_000, testScheduler.currentTime)
            assertEquals(DataResult.Success("data"), awaitItem())
            assertEquals(3_000, testScheduler.currentTime)
            awaitComplete()
        }
        assertEquals(3, subscriptions)
    }

    @Test
    fun `asDataResult backoff doubles and is capped at 30 seconds`() = runTest {
        val delays = mutableListOf<Long>()
        var last = 0L
        flow<Int> { throw IOException() }
            .asDataResult(mapToUnexpected)
            .onEach { delays += testScheduler.currentTime - last; last = testScheduler.currentTime }
            .take(8)
            .toList()
        assertEquals(listOf(0L, 1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), delays)
    }

    @Test
    fun `asDataResult backoff returns to 1 second after a Success`() = runTest {
        var subscriptions = 0
        val upstream = flow {
            subscriptions++
            when (subscriptions) {
                1 -> throw IOException("1")
                2 -> { emit(1); throw IOException("2") }
                else -> emit(2)
            }
        }
        val times = mutableListOf<Pair<Boolean, Long>>()
        upstream.asDataResult(mapToUnexpected)
            .onEach { times += (it is DataResult.Success) to testScheduler.currentTime }
            .toList()
        assertEquals(
            listOf(false to 0L, true to 1_000L, false to 1_000L, true to 2_000L),
            times,
        )
    }

    @Test
    fun `asDataResult rethrows CancellationException from upstream`() = runTest {
        assertCancelled {
            flow<Int> { throw CancellationException("cancelled") }.asDataResult(mapToUnexpected).toList()
        }
    }

    @Test
    fun `backoffMillis follows 1s 2s 4s 8s 16s then 30s`() {
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L, 30_000L),
            (0..7).map { backoffMillis(it) },
        )
        assertEquals(1_000L, backoffMillis(-3))
        assertEquals(30_000L, backoffMillis(Int.MAX_VALUE))
    }
}
