package com.khangtm.pokepoke.core.ui

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataSnapshot

/** What to display for data loaded network-first. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Content<T>(val data: T, val isOffline: Boolean) : LoadState<T>
    data class Error(val error: AppError) : LoadState<Nothing>
}

fun <T> AppResult<DataSnapshot<T>>.toLoadState(): LoadState<T> = when (this) {
    is AppResult.Success -> LoadState.Content(value.value, value.isOffline)
    is AppResult.Failure -> LoadState.Error(error)
}
