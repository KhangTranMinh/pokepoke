package com.khangtm.pokepoke.ui.common

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** One-shot event carried in a `UiState`; [id] makes two messages with the same text distinct. */
data class UiMessage(
    val text: UiText,
    val id: Long = System.nanoTime(),
) {
    constructor(@StringRes textRes: Int, args: List<Any> = emptyList(), id: Long = System.nanoTime()) :
        this(UiText.Res(textRes, args), id)
}

/** Shows [message] as a snackbar, then calls [onShown] so the ViewModel can clear it. */
@Composable
fun MessageEffect(
    message: UiMessage?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val text = message?.text?.asString()
    LaunchedEffect(message?.id) {
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            onShown()
        }
    }
}
