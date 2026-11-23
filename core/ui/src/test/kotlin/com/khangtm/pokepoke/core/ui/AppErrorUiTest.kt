package com.khangtm.pokepoke.core.ui

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataSnapshot
import com.khangtm.pokepoke.translation.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorUiTest {

    @Test
    fun `titleRes and messageRes map every AppError`() {
        assertEquals(R.string.ui_error_no_connection_title, AppError.NoConnection.titleRes())
        assertEquals(R.string.ui_error_no_connection_message, AppError.NoConnection.messageRes())
        assertEquals(R.string.ui_error_not_found_title, AppError.NotFound.titleRes())
        assertEquals(R.string.ui_error_not_found_message, AppError.NotFound.messageRes())
        assertEquals(R.string.ui_error_server_title, AppError.Server(500).titleRes())
        assertEquals(R.string.ui_error_server_message, AppError.Server(500).messageRes())
        assertEquals(R.string.ui_error_unknown_title, AppError.Unknown().titleRes())
        assertEquals(R.string.ui_error_unknown_message, AppError.Unknown(IllegalStateException()).messageRes())
    }

    @Test
    fun `toUiMessage uses the message resource without args`() {
        val message = AppError.NoConnection.toUiMessage()
        assertEquals(R.string.ui_error_no_connection_message, message.textRes)
        assertEquals(emptyList<Any>(), message.args)
    }

    @Test
    fun `toUiMessage passes the code of a Server error as argument`() {
        val message = AppError.Server(503).toUiMessage()
        assertEquals(R.string.ui_error_server_message, message.textRes)
        assertEquals(listOf<Any>(503), message.args)
    }

    @Test
    fun `UiMessage with different ids are not equal even with the same text`() {
        val a = UiMessage(R.string.ui_retry, id = 1)
        val b = UiMessage(R.string.ui_retry, id = 2)
        assertEquals(false, a == b)
        assertEquals(a, UiMessage(R.string.ui_retry, id = 1))
    }

    @Test
    fun `toLoadState maps Success to Content with the offline flag`() {
        assertEquals(
            LoadState.Content("data", isOffline = false),
            AppResult.Success(DataSnapshot("data", isOffline = false)).toLoadState(),
        )
        assertEquals(
            LoadState.Content("cached", isOffline = true),
            AppResult.Success(DataSnapshot("cached", isOffline = true)).toLoadState(),
        )
    }

    @Test
    fun `toLoadState maps Failure to Error`() {
        val result: AppResult<DataSnapshot<String>> = AppResult.Failure(AppError.NotFound)
        assertEquals(LoadState.Error(AppError.NotFound), result.toLoadState())
    }
}
