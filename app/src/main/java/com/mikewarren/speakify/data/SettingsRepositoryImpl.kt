// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.NotificationSourceModel
import com.mikewarren.speakify.data.db.UserAppModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject


class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) : SettingsRepository {
    private val _db = DbProvider.GetDb(context)

    override val appSettings: Flow<Map<String, AppSettingsModel>> = _db.appSettingsDao().getAllFlow()
        .map { list ->
            // Map the list from the DB into your Map<PackageName, Model> structure
            list.mapNotNull { nestedModel ->
                val model = AppSettingsModel.FromDbModel(nestedModel)
                if (model != null) {
                    return@mapNotNull model.packageName to model
                }
                return@mapNotNull null
            }.toMap()
        }
        // We use stateIn to keep the latest value cached (hot flow), similar to your previous behavior
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO), // Keep it alive as long as the Repo is alive (Singleton)
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    override val useDarkTheme: Flow<Boolean?> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.useDarkTheme
        }

    override val selectedTTSVoice: Flow<String?> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.selectedTTSVoice
        }

    override val maximizeVolumeOnScreenOff: Flow<Boolean> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.maximizeVolumeOnScreenOff
        }

    override val stopSpeechOnScreenOff: Flow<Boolean> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.stopSpeechOnScreenOff
        }

    override val minVolume: Flow<Int> = userSettingsDataStore
        .data
        .map { model: UserSettingsModel ->
            model.minVolume
        }

    override val isCrashlyticsEnabled: Flow<Boolean> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.isCrashlyticsEnabled
        }

    override val originalVolume: Flow<Int> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.originalVolume
        }

    override suspend fun updateUseDarkTheme(useDarkTheme: Boolean) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(useDarkTheme = useDarkTheme)
        }
    }

    override suspend fun saveSelectedVoice(voiceName: String) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(selectedTTSVoice = voiceName)
        }
    }

    override suspend fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(maximizeVolumeOnScreenOff = shouldMaximize)
        }
    }

    override suspend fun setStopSpeechOnScreenOff(shouldStop: Boolean) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(stopSpeechOnScreenOff = shouldStop)
        }
    }

    override suspend fun setMinVolume(volume: Int) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(minVolume = volume)
        }
    }

    override suspend fun setCrashlyticsEnabled(isEnabled: Boolean) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(isCrashlyticsEnabled = isEnabled)
        }
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isEnabled)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to update Crashlytics status", e)
        }
    }

    override suspend fun setOriginalVolume(volume: Int) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(originalVolume = volume)
        }
    }

    override suspend fun saveAppSettings(appSettingsModel: AppSettingsModel) {
        val appSettingsDao = _db.appSettingsDao()
        val notificationSourcesDao = _db.notificationSourcesDao()
        val userAppsDao = _db.userAppsDao()

        // Ensure the parent app exists in the 'important_apps' table to satisfy Foreign Key constraints.
        // This is especially important during data restoration if 'app_settings' are restored before
        // or without corresponding 'important_apps'.
        var userApp = userAppsDao.getByPackageName(appSettingsModel.packageName)
        if (userApp == null) {
            val newId = userAppsDao.insertIgnore(
                UserAppModel(
                    packageName = appSettingsModel.packageName,
                    appName = appSettingsModel.packageName // Default to package name if not present
                )
            )
            userApp = userAppsDao.getByPackageName(appSettingsModel.packageName)
        }

        val appSettingsDbModel = AppSettingsDbModel(
            id = appSettingsModel.id,
            userAppId = userApp?.id,
            announcerVoice = appSettingsModel.announcerVoice,
            additionalSettings = appSettingsModel.additionalSettings,
        )

        if (appSettingsModel.id != null) {
            notificationSourcesDao.deleteAllWithoutValues(appSettingsModel.id, appSettingsModel.notificationSources.map { it.value })
        }

        val savedAppSettingsId = saveToDatabase(appSettingsDao, appSettingsDbModel, appSettingsModel.packageName)

        notificationSourcesDao.insertAll(appSettingsModel.notificationSources.map { source ->
            NotificationSourceModel(
                id = null,
                appSettingsId = savedAppSettingsId,
                value = source.value,
                name = source.name
            )
        })

    }

    private suspend fun saveToDatabase(appSettingsDao: AppSettingsDao, appSettingsDbModel: AppSettingsDbModel, packageName: String) : Long {
        if (appSettingsDbModel.id == null) {
            // Try to find if we already have an entry for this package to avoid duplicate entries with different IDs
            val existing = appSettingsDao.getByPackageName(packageName)
            if (existing != null) {
                val updatedModel = appSettingsDbModel.copy(id = existing.appSettings.id)
                appSettingsDao.updateAppSettings(updatedModel)
                return existing.appSettings.id!!
            }
            return appSettingsDao.insert(appSettingsDbModel)

        }
        appSettingsDao.updateAppSettings(appSettingsDbModel)
        return appSettingsDbModel.id
    }

    override fun getContext() : Context {
        return context
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            _db.clearAllTables()
        }
        userSettingsDataStore.updateData { UserSettingsModel() }
    }

}
