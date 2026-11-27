package com.khangtm.pokepoke.ui.common

import androidx.annotation.StringRes
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.ui.translation.R

@StringRes
internal fun AppError.titleRes(): Int = when (this) {
    AppError.NoConnection -> R.string.ui_error_no_connection_title
    AppError.NotFound -> R.string.ui_error_not_found_title
    is AppError.Server -> R.string.ui_error_server_title
    is AppError.Unknown -> R.string.ui_error_unknown_title
}

@StringRes
internal fun AppError.messageRes(): Int = when (this) {
    AppError.NoConnection -> R.string.ui_error_no_connection_message
    AppError.NotFound -> R.string.ui_error_not_found_message
    is AppError.Server -> R.string.ui_error_server_message
    is AppError.Unknown -> R.string.ui_error_unknown_message
}

/** Format arguments of [messageRes]: `Server` passes its code. */
private fun AppError.messageArgs(): List<Any> = when (this) {
    is AppError.Server -> listOf(code)
    else -> emptyList()
}

fun AppError.titleText(): UiText = UiText.Res(titleRes())

/** `Server` with a [AppError.Server.serverMessage] shows that message as it is; the others use [messageRes]. */
fun AppError.messageText(): UiText {
    val serverMessage = (this as? AppError.Server)?.serverMessage
    return if (serverMessage != null) UiText.Raw(serverMessage) else UiText.Res(messageRes(), messageArgs())
}

fun AppError.toUiMessage(): UiMessage = UiMessage(messageText())
