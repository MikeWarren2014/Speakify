package com.mikewarren.speakify.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.NotificationSourceModel
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.db.UserAppsDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

// 1. Define a serializable data class to hold all your app data
@Serializable
data class BackupData(
    val userApps: List<UserAppModel>,
    val appSettings: List<AppSettingsDbModel>,
    val notificationSources: List<NotificationSourceModel>,
    // user settings
    val userSettingsModel: UserSettingsModel,
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userAppsDao: UserAppsDao,
    private val appSettingsDao: AppSettingsDao,
    private val notificationSourcesDao: NotificationSourcesDao,

    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportData(uri: Uri): Result<Boolean> {
        return try {
            // 1. Gather data from all tables
            val apps = userAppsDao.getAll()
            val settings = appSettingsDao.getAllRaw()
            val notificationSources = notificationSourcesDao.getAll()
            val userSettingsModel = userSettingsDataStore.data.first()


            val backupData = BackupData(
                apps,
                settings,
                notificationSources,
                userSettingsModel,
            )

            // 2. Serialize to JSON
            val jsonString = json.encodeToString(backupData)

            // 3. Write to the file selected by the user
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e("BackupRepo", "Export failed", e)
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<Boolean> {
        return try {
            // 1. Read the file content
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }

            // 2. Deserialize JSON
            val backupData = json.decodeFromString<BackupData>(stringBuilder.toString())

            // 3. Insert data back into DB (Handle duplicates or clear old data first)

            // Option B: Insert/Ignore conflicts (Safer)
            backupData.userApps.forEach { userAppsDao.insert(it) }
            backupData.appSettings.forEach { appSettingsDao.insert(it) }
            notificationSourcesDao.insertAll(backupData.notificationSources)

            userSettingsDataStore.updateData { backupData.userSettingsModel }


            Result.success(true)
        } catch (e: Exception) {
            Log.e("BackupRepo", "Import failed", e)
            Result.failure(e)
        }
    }
}