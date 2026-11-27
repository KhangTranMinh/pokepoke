package com.khangtm.pokepoke.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text that is not resolved yet, so a ViewModel / `UiState` can carry it without a `Context`.
 * [Raw] is text that already exists (server message, data); [Res] is a string resource.
 * The View does not care which one it gets: resolve with [asString].
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText

    /** [args] that are [UiText] are resolved first (nested), the others are passed to the format as they are. */
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
}

/** The only place a [UiText] becomes a `String`. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> {
        val resolved = args.map { if (it is UiText) it.asString() else it }
        stringResource(id, *resolved.toTypedArray())
    }
}
