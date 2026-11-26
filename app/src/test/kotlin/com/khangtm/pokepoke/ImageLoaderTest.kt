package com.khangtm.pokepoke

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImageLoaderTest {

    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `image loader is built with an okhttp fetcher`() {
        val loader = createImageLoader(context, OkHttpClient())

        assertNotNull(loader)
        assertNotNull(loader.components.fetcherFactories.firstOrNull())
    }

    @Test
    fun `application builds its image loader`() {
        val application = PokePokeApplication()

        assertNotNull(application.newImageLoader(context))
    }

    @Test
    fun `application image loader is built each time from the same http client`() {
        val application = PokePokeApplication()

        val first = application.newImageLoader(context)
        val second = application.newImageLoader(context)

        assertNotNull(first)
        assertNotNull(second)
        assertSame(first.components.fetcherFactories.size, second.components.fetcherFactories.size)
    }
}
