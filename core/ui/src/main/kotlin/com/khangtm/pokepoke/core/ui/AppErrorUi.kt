package com.khangtm.pokepoke.core.ui

import androidx.annotation.StringRes
import com.khangtm.pokepoke.core.common.AppError

@StringRes
fun AppError.titleRes(): Int = when (this) {
    AppError.NoConnection -> R.string.ui_error_no_connection_title
    AppError.NotFound -> R.string.ui_error_not_found_title
    is AppError.Server -> R.string.ui_error_server_title
    is AppError.Unknown -> R.string.ui_error_unknown_title
}

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.NoConnection -> R.string.ui_error_no_connection_message
    AppError.NotFound -> R.string.ui_error_not_found_message
    is AppError.Server -> R.string.ui_error_server_message
    is AppError.Unknown -> R.string.ui_error_unknown_message
}

/** Format arguments of [messageRes]: `Server` passes its code. */
internal fun AppError.messageArgs(): List<Any> = when (this) {
    is AppError.Server -> listOf(code)
    else -> emptyList()
}

fun AppError.toUiMessage(): UiMessage = UiMessage(textRes = messageRes(), args = messageArgs())
