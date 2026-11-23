package com.khangtm.pokepoke.core.network

import org.junit.Assert.assertSame
import org.junit.Assert.assertNotNull
import java.lang.reflect.Proxy
import org.junit.Test

class ApiServiceFactoryTest {

    private interface SampleService

    @Test
    fun `reified create asks for the service class`() {
        var requested: Class<*>? = null
        val factory = object : ApiServiceFactory {
            override fun <T : Any> create(service: Class<T>): T {
                requested = service
                return service.cast(Proxy.newProxyInstance(service.classLoader, arrayOf(service)) { _, _, _ -> null })!!
            }
        }

        val service = factory.create<SampleService>()

        assertSame(SampleService::class.java, requested)
        assertNotNull(service)
    }
}
