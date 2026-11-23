package com.khangtm.pokepoke.core.storage.impl

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.khangtm.pokepoke.core.common.IoDispatcher
import com.khangtm.pokepoke.core.storage.api.KeyValueStore
import com.khangtm.pokepoke.core.storage.api.KeyValueStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal const val STORE_FILE_EXTENSION = ".preferences_pb"

/** Files with this prefix belong to `:core:securestorage` and are never reported here. */
internal const val SECURE_PREFIX = "secure_"

private val VALID_NAME = Regex("[A-Za-z0-9_.-]+")

/**
 * One DataStore file per name in [directory]. DataStore allows a single active instance per file, so instances
 * are cached by name.
 */
@Singleton
internal class KeyValueStoreFactoryImpl(
    private val directory: File,
    private val scope: CoroutineScope,
) : KeyValueStoreFactory {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ) : this(File(context.filesDir, "datastore"), CoroutineScope(SupervisorJob() + dispatcher))

    private val stores = ConcurrentHashMap<String, KeyValueStoreImpl>()

    override fun create(name: String): KeyValueStore {
        require(VALID_NAME.matches(name)) { "Invalid store name '$name'" }
        require(!name.startsWith(SECURE_PREFIX)) { "Store name must not start with '$SECURE_PREFIX': '$name'" }
        return stores.computeIfAbsent(name) {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
                File(directory, "$name$STORE_FILE_EXTENSION")
            }
            KeyValueStoreImpl(name, dataStore)
        }
    }

    override fun storeNames(): List<String> =
        directory.listFiles().orEmpty()
            .map { it.name }
            .filter { it.endsWith(STORE_FILE_EXTENSION) && !it.startsWith(SECURE_PREFIX) }
            .map { it.removeSuffix(STORE_FILE_EXTENSION) }
            .sorted()
}
