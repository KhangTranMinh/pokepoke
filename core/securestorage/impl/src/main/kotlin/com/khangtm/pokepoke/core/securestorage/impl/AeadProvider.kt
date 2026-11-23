package com.khangtm.pokepoke.core.securestorage.impl

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Source of the [Aead] that encrypts values. Replaced by a cleartext keyset in unit tests (no Android Keystore). */
internal interface AeadProvider {
    fun aead(): Aead
}

private const val KEYSET_NAME = "pokepoke_tink_keyset"
private const val KEYSET_PREFS = "pokepoke_tink_prefs"
private const val MASTER_KEY_URI = "android-keystore://pokepoke_master_key"

/** AES-256-GCM keyset kept in SharedPreferences, wrapped by a master key in the Android Keystore. */
@Singleton
internal class TinkAeadProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AeadProvider {

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun aead(): Aead = aead
}
