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
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseDbTest: DbProviderMocker {
    protected val testDispatcher = StandardTestDispatcher()

    override val context = ApplicationProvider.getApplicationContext<Context>()

    override lateinit var db: AppDatabase
    protected lateinit var fakeFirestore: FakeFirestore

    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        every { Dispatchers.Default } returns testDispatcher
        every { Dispatchers.Unconfined } returns testDispatcher

        setUpDatabaseDoubles()
    }

    override fun setUpDatabaseDoubles() {
        super.setUpDatabaseDoubles()

        fakeFirestore = FakeFirestore()

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns fakeFirestore.mock

    }

    @After
    open fun tearDown() {
        tearDownDatabaseDoubles()
        Dispatchers.resetMain()
        unmockkStatic(Dispatchers::class)
        unmockkStatic(FirebaseFirestore::class)
    }
}
