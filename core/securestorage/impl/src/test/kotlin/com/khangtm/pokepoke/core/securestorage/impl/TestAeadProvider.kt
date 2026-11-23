package com.khangtm.pokepoke.core.securestorage.impl

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig

/** AES-256-GCM from a freshly generated cleartext keyset: no Android Keystore needed in unit tests. */
@Suppress("DEPRECATION") // generateNew(KeyTemplate), as in contracts.md §4
internal class TestAeadProvider : AeadProvider {
    private val aead: Aead by lazy {
        AeadConfig.register()
        KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM")).getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun aead(): Aead = aead
}
