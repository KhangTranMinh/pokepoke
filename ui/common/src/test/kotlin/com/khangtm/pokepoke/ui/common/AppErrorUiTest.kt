package com.khangtm.pokepoke.ui.common

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataSnapshot
import com.khangtm.pokepoke.ui.translation.R
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
    fun `titleText wraps the title resource`() {
        assertEquals(UiText.Res(R.string.ui_error_no_connection_title), AppError.NoConnection.titleText())
        assertEquals(UiText.Res(R.string.ui_error_server_title), AppError.Server(500, "Maintenance").titleText())
    }

    @Test
    fun `messageText of Server without serverMessage passes the code`() {
        assertEquals(UiText.Res(R.string.ui_error_server_message, listOf(503)), AppError.Server(503).messageText())
    }

    @Test
    fun `messageText of Server with serverMessage is that message`() {
        assertEquals(UiText.Raw("Maintenance"), AppError.Server(500, "Maintenance").messageText())
    }

    @Test
    fun `messageText of the other errors is the message resource without args`() {
        assertEquals(UiText.Res(R.string.ui_error_no_connection_message), AppError.NoConnection.messageText())
        assertEquals(UiText.Res(R.string.ui_error_not_found_message), AppError.NotFound.messageText())
        assertEquals(UiText.Res(R.string.ui_error_unknown_message), AppError.Unknown().messageText())
    }

    @Test
    fun `toUiMessage wraps messageText`() {
        assertEquals(AppError.NoConnection.messageText(), AppError.NoConnection.toUiMessage().text)
        assertEquals(UiText.Raw("Maintenance"), AppError.Server(500, "Maintenance").toUiMessage().text)
        assertEquals(
            UiText.Res(R.string.ui_error_server_message, listOf(503)),
            AppError.Server(503).toUiMessage().text,
        )
    }

    @Test
    fun `UiMessage secondary constructor builds a Res text`() {
        assertEquals(UiMessage(UiText.Res(R.string.ui_retry, listOf(1)), id = 3), UiMessage(R.string.ui_retry, listOf(1), id = 3))
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
