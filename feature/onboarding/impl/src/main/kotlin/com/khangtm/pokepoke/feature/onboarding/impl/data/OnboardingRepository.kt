package com.khangtm.pokepoke.feature.onboarding.impl.data

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.asDataResult
import com.khangtm.pokepoke.core.common.dataResultOf
import com.khangtm.pokepoke.core.storage.api.KeyValueStoreFactory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Owns the "onboarding completed" flag (docs/design/contracts.md §9). */
internal interface OnboardingRepository {
    /** Emits `false` while the flag was never written. */
    fun observeCompleted(): Flow<DataResult<Boolean>>

    suspend fun markCompleted(): DataResult<Unit>
}

internal class OnboardingRepositoryImpl @Inject constructor(
    storeFactory: KeyValueStoreFactory,
) : OnboardingRepository {

    private val store = storeFactory.create(STORE_NAME)

    override fun observeCompleted(): Flow<DataResult<Boolean>> =
        store.observeBoolean(KEY_COMPLETED)
            .map { it ?: false }
            .asDataResult { DataError.Local.Read(it) }

    override suspend fun markCompleted(): DataResult<Unit> =
        dataResultOf({ DataError.Local.Write(it) }) { store.putBoolean(KEY_COMPLETED, true) }

    internal companion object {
        const val STORE_NAME = "onboarding"
        const val KEY_COMPLETED = "completed"
    }
}
