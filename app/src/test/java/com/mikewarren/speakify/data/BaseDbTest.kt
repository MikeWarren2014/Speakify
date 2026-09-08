package com.mikewarren.speakify.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.fakes.FakeFirestore
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseDbTest {
    private val testDispatcher = StandardTestDispatcher()

    protected val context = ApplicationProvider.getApplicationContext<Context>()

    protected lateinit var db: AppDatabase
    protected lateinit var fakeFirestore: FakeFirestore

    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        setUpDatabaseDoubles()
    }

    private fun setUpDatabaseDoubles() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        mockkObject(DbProvider)
        every { DbProvider.GetDb(any()) } returns db

        fakeFirestore = FakeFirestore()

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns fakeFirestore.mock

    }

    @After
    open fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }
}