package com.khangtm.pokepoke.core.securestorage.impl

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.khangtm.pokepoke.core.common.IoDispatcher
import com.khangtm.pokepoke.core.securestorage.api.SecureStorage
import com.khangtm.pokepoke.core.securestorage.api.SecureStorageFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val SECURE_PREFIX = "secure_"
private const val STORE_FILE_EXTENSION = ".preferences_pb"
private val VALID_NAME = Regex("[A-Za-z0-9_.-]+")

/**
 * One DataStore file `secure_<name>.preferences_pb` per name, in the same directory as `:core:storage` (which
 * skips the `secure_` files). DataStore allows a single active instance per file, so instances are cached by name.
 */
@Singleton
internal class SecureStorageFactoryImpl(
    private val directory: File,
    private val scope: CoroutineScope,
    private val aeadProvider: AeadProvider,
) : SecureStorageFactory {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        aeadProvider: AeadProvider,
    ) : this(File(context.filesDir, "datastore"), CoroutineScope(SupervisorJob() + dispatcher), aeadProvider)

    private val storages = ConcurrentHashMap<String, SecureStorageImpl>()

    override fun create(name: String): SecureStorage {
        require(VALID_NAME.matches(name)) { "Invalid store name '$name'" }
        return storages.computeIfAbsent(name) {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
                File(directory, "$SECURE_PREFIX$name$STORE_FILE_EXTENSION")
            }
            SecureStorageImpl(name, dataStore, aeadProvider)
        }
    }

    override fun storeNames(): List<String> =
        directory.listFiles().orEmpty()
            .map { it.name }
            .filter { it.startsWith(SECURE_PREFIX) && it.endsWith(STORE_FILE_EXTENSION) }
            .map { it.removePrefix(SECURE_PREFIX).removeSuffix(STORE_FILE_EXTENSION) }
            .sorted()
}
