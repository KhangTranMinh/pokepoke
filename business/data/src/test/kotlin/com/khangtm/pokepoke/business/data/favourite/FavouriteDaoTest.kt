package com.khangtm.pokepoke.business.data.favourite

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.business.data.favourite.db.FavouriteDao
import com.khangtm.pokepoke.business.data.favourite.db.FavouriteDatabase
import com.khangtm.pokepoke.business.data.favourite.db.FavouriteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouriteDaoTest {

    private lateinit var database: FavouriteDatabase
    private lateinit var dao: FavouriteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FavouriteDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.favouriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `rows are ordered by added_at, newest first`() = runTest {
        dao.toggle(1, addedAt = 300)
        dao.toggle(2, addedAt = 100)
        dao.toggle(3, addedAt = 200)
        assertEquals(
            listOf(FavouriteEntity(1, 300), FavouriteEntity(3, 200), FavouriteEntity(2, 100)),
            dao.observeAll().first(),
        )
    }

    @Test
    fun `equal added_at keeps a stable order`() = runTest {
        dao.toggle(1, addedAt = 100)
        dao.toggle(2, addedAt = 100)
        assertEquals(listOf(2, 1), dao.observeAll().first().map { it.pokemonId })
    }

    @Test
    fun `toggle adds with the given time and a second toggle removes`() = runTest {
        assertEquals(true, dao.toggle(5, addedAt = 42))
        assertEquals(listOf(FavouriteEntity(5, 42)), dao.observeAll().first())
        assertEquals(true, dao.observeExists(5).first())
        assertEquals(false, dao.toggle(5, addedAt = 43))
        assertEquals(emptyList<FavouriteEntity>(), dao.observeAll().first())
        assertEquals(false, dao.observeExists(5).first())
    }
}
