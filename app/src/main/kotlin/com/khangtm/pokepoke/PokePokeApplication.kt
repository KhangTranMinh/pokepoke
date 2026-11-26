package com.khangtm.pokepoke

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Call
import okhttp3.OkHttpClient

@HiltAndroidApp
class PokePokeApplication : Application(), SingletonImageLoader.Factory {

    // A separate client from the API one (:core:network): images go through no debug interceptor.
    private val imageHttpClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        createImageLoader(context, imageHttpClient)
}

internal fun createImageLoader(context: PlatformContext, httpClient: Call.Factory): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { httpClient })) }
        .crossfade(true)
        .build()
