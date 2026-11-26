package com.khangtm.pokepoke

import com.khangtm.pokepoke.di.AppModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModuleTest {

    @Test
    fun `base url is the flavor BASE_URL and ends with a slash`() {
        val baseUrl = AppModule.provideBaseUrl()

        assertEquals(BuildConfig.BASE_URL, baseUrl)
        assertTrue(baseUrl.startsWith("https://"))
        assertTrue(baseUrl.endsWith("/"))
    }

    @Test
    fun `deep link config is the flavor scheme and host from BuildConfig`() {
        val config = AppModule.provideDeepLinkConfig()

        assertEquals(setOf(BuildConfig.DEEPLINK_SCHEME), config.schemes)
        assertEquals(setOf(BuildConfig.DEEPLINK_HOST), config.hosts)
    }
}
