package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.UserAppsDao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mikewarren.speakify.data.db.UserAppModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class UserAppDaoTest {

    private lateinit var userAppDao: UserAppsDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        userAppDao = db.userAppsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetAllUserApps() = runTest {
        val userApp = UserAppModel("com.example.app1", "App 1", true)
        userAppDao.insertAll(userApp)
        val allApps = userAppDao.getAll()

        assertTrue(allApps.contains(userApp))
    }

    // Add more test methods for your DAO operations
}