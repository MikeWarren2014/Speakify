package com.mikewarren.speakify.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.NotificationSourceModel
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.db.UserAppsDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AppSettingsDaoTest {
    private lateinit var userAppDao: UserAppsDao
    private lateinit var appSettingsDao: AppSettingsDao

    private lateinit var notificationSourcesDao: NotificationSourcesDao

    private lateinit var db: AppDatabase

    companion object {
        const val PhoneAppPackageName = "com.google.android.dialer"
    }

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
        appSettingsDao = db.appSettingsDao()
        notificationSourcesDao = db.notificationSourcesDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private suspend fun insertTestApp(): Long {
        val userApp = UserAppModel(packageName = PhoneAppPackageName, appName = "Phone")
        userAppDao.insert(userApp)
        return userAppDao.getByPackageName(PhoneAppPackageName)!!.id!!
    }

    @Test
    @Throws(Exception::class)
    fun testInsertUpdatesId() = runTest {
        val uaId = insertTestApp()
        val appSettingsDbModel = AppSettingsDbModel(
            id = null,
            userAppId = uaId,
            announcerVoice = "en-US-language-male",
        )
        val appSettingsId = appSettingsDao.insert(appSettingsDbModel)
        assertNotEquals(0, appSettingsId)
        assertNotNull(appSettingsId)
    }

    @Test
    @Throws(Exception::class)
    fun testInsertAlreadyExisting() = runTest {
        val uaId = insertTestApp()
        val appSettingsDbModel = AppSettingsDbModel(
            id = null,
            userAppId = uaId,
            announcerVoice = "en-US-language-male",
        )
        appSettingsDao.insert(appSettingsDbModel)
        appSettingsDao.insert(appSettingsDbModel)

        assert(appSettingsDao.getAll().size == 1)
        assertEquals(1L, appSettingsDao.getAll().first().appSettings.id)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByPackageName() = runTest {
        val uaId = insertTestApp()
        val appSettingsDbModel = AppSettingsDbModel(
            id = null,
            userAppId = uaId,
            announcerVoice = "en-US-language-male",
        )
        val asId = appSettingsDao.insert(appSettingsDbModel)

        notificationSourcesDao.insertAll(listOf(
            NotificationSourceModel(
                id = null,
                appSettingsId = asId,
                value = "test",
            ),
            NotificationSourceModel(
                id = null,
                appSettingsId = asId,
                value = "test2",
            ),
        ))

        val result = appSettingsDao.getByPackageName(PhoneAppPackageName)
        assertNotNull(result)
        assertEquals(2, result?.notificationSources?.size)
    }
}
