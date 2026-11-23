package com.khangtm.pokepoke.core.ui

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource

/** One-shot event carried in a `UiState`; [id] makes two messages with the same text distinct. */
data class UiMessage(
    @param:StringRes val textRes: Int,
    val args: List<Any> = emptyList(),
    val id: Long = System.nanoTime(),
)

/** Shows [message] as a snackbar, then calls [onShown] so the ViewModel can clear it. */
@Composable
fun MessageEffect(
    message: UiMessage?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val text = message?.let { stringResource(it.textRes, *it.args.toTypedArray()) }
    LaunchedEffect(message?.id) {
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            onShown()
        }
    }
}
