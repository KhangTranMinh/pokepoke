package com.khangtm.pokepoke.core.common

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NetworkFirstTest {

    private val parseError = DataError.Network.Parse(IllegalArgumentException("bad"))

    // ---- runNetworkFirst ----

    @Test
    fun `runNetworkFirst offline by monitor does not call fetch`() = runTest {
        var fetched = false
        val outcome = runNetworkFirst(isOnline = { false }) { fetched = true; DataResult.Success(Unit) }
        assertEquals(FetchOutcome.Offline, outcome)
        assertTrue(!fetched)
    }

    @Test
    fun `runNetworkFirst online and fetch succeeds is Fetched`() = runTest {
        assertEquals(FetchOutcome.Fetched, runNetworkFirst({ true }) { DataResult.Success(Unit) })
    }

    @Test
    fun `runNetworkFirst NoConnection and Timeout are Offline`() = runTest {
        assertEquals(
            FetchOutcome.Offline,
            runNetworkFirst({ true }) { DataResult.Failure(DataError.Network.NoConnection) },
        )
        assertEquals(
            FetchOutcome.Offline,
            runNetworkFirst({ true }) { DataResult.Failure(DataError.Network.Timeout) },
        )
    }

    @Test
    fun `runNetworkFirst other errors are Failed`() = runTest {
        listOf(
            DataError.Network.Http(500),
            parseError,
            DataError.NotFound,
            DataError.Local.Write(IOException()),
            DataError.Unexpected(IOException()),
        ).forEach { error ->
            assertEquals(FetchOutcome.Failed(error), runNetworkFirst({ true }) { DataResult.Failure(error) })
        }
    }

    // ---- networkFirst ----

    @Test
    fun `online success emits cache as not offline`() = runTest {
        networkFirst(
            isOnline = { true },
            fetch = { DataResult.Success(Unit) },
            observe = { flowOf(DataResult.Success("fresh")) },
        ).test {
            assertEquals(AppResult.Success(DataSnapshot("fresh", isOffline = false)), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `offline by monitor emits cache as offline without fetching`() = runTest {
        var fetched = false
        networkFirst(
            isOnline = { false },
            fetch = { fetched = true; DataResult.Success(Unit) },
            observe = { flowOf(DataResult.Success("cached")) },
        ).test {
            assertEquals(AppResult.Success(DataSnapshot("cached", isOffline = true)), awaitItem())
            awaitComplete()
        }
        assertTrue(!fetched)
    }

    @Test
    fun `NoConnection and Timeout fall back to the cache as offline`() = runTest {
        listOf(DataError.Network.NoConnection, DataError.Network.Timeout).forEach { error ->
            networkFirst(
                isOnline = { true },
                fetch = { DataResult.Failure(error) },
                observe = { flowOf(DataResult.Success("cached")) },
            ).test {
                assertEquals(AppResult.Success(DataSnapshot("cached", isOffline = true)), awaitItem())
                awaitComplete()
            }
        }
    }

    @Test
    fun `offline without cache turns NotFound into NoConnection`() = runTest {
        networkFirst<String>(
            isOnline = { false },
            fetch = { DataResult.Success(Unit) },
            observe = { flowOf(DataResult.Failure(DataError.NotFound)) },
        ).test {
            assertEquals(AppResult.Failure(AppError.NoConnection), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `offline other cache errors use the default mapping`() = runTest {
        val cause = IOException("disk")
        networkFirst<String>(
            isOnline = { false },
            fetch = { DataResult.Success(Unit) },
            observe = { flowOf(DataResult.Failure(DataError.Local.Read(cause))) },
        ).test {
            assertEquals(AppResult.Failure(AppError.Unknown(cause)), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `online cache errors use the default mapping and NotFound stays NotFound`() = runTest {
        networkFirst<String>(
            isOnline = { true },
            fetch = { DataResult.Success(Unit) },
            observe = { flowOf(DataResult.Failure(DataError.NotFound)) },
        ).test {
            assertEquals(AppResult.Failure(AppError.NotFound), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Http error fails without reading the cache`() = runTest {
        var observed = false
        networkFirst<String>(
            isOnline = { true },
            fetch = { DataResult.Failure(DataError.Network.Http(500)) },
            observe = { observed = true; flowOf(DataResult.Success("cached")) },
        ).test {
            assertEquals(AppResult.Failure(AppError.Server(500)), awaitItem())
            awaitComplete()
        }
        assertTrue(!observed)
    }

    @Test
    fun `Parse error fails without reading the cache`() = runTest {
        var observed = false
        networkFirst<String>(
            isOnline = { true },
            fetch = { DataResult.Failure(parseError) },
            observe = { observed = true; flowOf(DataResult.Success("cached")) },
        ).test {
            assertEquals(AppResult.Failure(AppError.Unknown(parseError.cause)), awaitItem())
            awaitComplete()
        }
        assertTrue(!observed)
    }

    @Test
    fun `first emission is already the final data and the cache is not read before fetch`() = runTest {
        val events = mutableListOf<String>()
        val cache = MutableStateFlow("old")
        networkFirst(
            isOnline = { true },
            fetch = { events += "fetch"; cache.value = "new"; DataResult.Success(Unit) },
            observe = { events += "observe"; flow { cache.collect { emit(DataResult.Success(it)) } } },
        ).test {
            assertEquals(AppResult.Success(DataSnapshot("new", isOffline = false)), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("fetch", "observe"), events)
    }

    @Test
    fun `later cache updates keep being emitted with the same offline flag`() = runTest {
        val cache = MutableStateFlow("v1")
        networkFirst(
            isOnline = { false },
            fetch = { DataResult.Success(Unit) },
            observe = { flow { cache.collect { emit(DataResult.Success(it)) } } },
        ).test {
            assertEquals(AppResult.Success(DataSnapshot("v1", isOffline = true)), awaitItem())
            cache.value = "v2"
            assertEquals(AppResult.Success(DataSnapshot("v2", isOffline = true)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
