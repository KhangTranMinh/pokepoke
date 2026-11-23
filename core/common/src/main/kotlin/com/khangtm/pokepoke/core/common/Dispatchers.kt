package com.khangtm.pokepoke.core.common

import javax.inject.Qualifier

/** Injected in place of `Dispatchers.IO` so tests can replace it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Injected in place of `Dispatchers.Default`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** A `CoroutineScope` that lives as long as the app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
