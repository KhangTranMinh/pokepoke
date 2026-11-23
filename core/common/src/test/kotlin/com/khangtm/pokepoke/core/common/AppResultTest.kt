package com.khangtm.pokepoke.core.common

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    private val cause = IllegalStateException("cause")

    @Test
    fun `toAppError maps every DataError`() {
        assertEquals(AppError.NoConnection, DataError.Network.NoConnection.toAppError())
        assertEquals(AppError.NoConnection, DataError.Network.Timeout.toAppError())
        assertEquals(AppError.NotFound, DataError.NotFound.toAppError())
        assertEquals(AppError.Server(500), DataError.Network.Http(500).toAppError())
        assertEquals(AppError.Unknown(cause), DataError.Network.Parse(cause).toAppError())
        assertEquals(AppError.Unknown(cause), DataError.Local.Read(cause).toAppError())
        assertEquals(AppError.Unknown(cause), DataError.Local.Write(cause).toAppError())
        assertEquals(AppError.Unknown(cause), DataError.Unexpected(cause).toAppError())
    }

    @Test
    fun `AppError Unknown cause defaults to null`() {
        assertEquals(null, AppError.Unknown().cause)
    }

    @Test
    fun `DataResult toAppResult maps Success and Failure`() {
        assertEquals(AppResult.Success(1), DataResult.Success(1).toAppResult())
        assertEquals(
            AppResult.Failure(AppError.Server(503)),
            DataResult.Failure(DataError.Network.Http(503)).toAppResult(),
        )
    }

    @Test
    fun `DataResult toAppResult accepts a custom error mapping`() {
        val result = DataResult.Failure(DataError.NotFound).toAppResult { AppError.NoConnection }
        assertEquals(AppResult.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `Flow toAppResult maps each element`() = runTest {
        val flow = flowOf<DataResult<Int>>(DataResult.Success(1), DataResult.Failure(DataError.NotFound))
        assertEquals(
            listOf(AppResult.Success(1), AppResult.Failure(AppError.NotFound)),
            flow.toAppResult().toList(),
        )
        assertEquals(
            listOf(AppResult.Success(1), AppResult.Failure(AppError.NoConnection)),
            flow.toAppResult { AppError.NoConnection }.toList(),
        )
    }

    @Test
    fun `map transforms Success and keeps Failure`() {
        assertEquals(AppResult.Success("1"), AppResult.Success(1).map { it.toString() })
        val failure: AppResult<Int> = AppResult.Failure(AppError.NotFound)
        assertSame(failure, failure.map { it + 1 })
    }

    @Test
    fun `onSuccess runs only for Success and returns the receiver`() {
        var seen: Int? = null
        val success: AppResult<Int> = AppResult.Success(5)
        assertSame(success, success.onSuccess { seen = it })
        assertEquals(5, seen)

        seen = null
        AppResult.Failure(AppError.NotFound).onSuccess { seen = 1 }
        assertEquals(null, seen)
    }

    @Test
    fun `onFailure runs only for Failure and returns the receiver`() {
        var seen: AppError? = null
        val failure: AppResult<Int> = AppResult.Failure(AppError.Server(500))
        assertSame(failure, failure.onFailure { seen = it })
        assertEquals(AppError.Server(500), seen)

        seen = null
        AppResult.Success(1).onFailure { seen = it }
        assertTrue(seen == null)
    }
}
