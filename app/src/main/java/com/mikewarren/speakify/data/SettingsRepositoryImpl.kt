// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.AppSettingsWithNotificationSources
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.NotificationSourceModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

        val appSettingsDbModel = AppSettingsDbModel(
            id = appSettingsModel.id,
            packageName = appSettingsModel.packageName,
            announcerVoice = appSettingsModel.announcerVoice,
        )

        if (appSettingsModel.id != null) {
            notificationSourcesDao.deleteAllWithoutValues(appSettingsModel.id, appSettingsModel.notificationSources)
        }

        val savedAppSettingsId = saveToDatabase(appSettingsDao, appSettingsDbModel)

        notificationSourcesDao.insertAll(appSettingsModel.notificationSources.map { value: String ->
            NotificationSourceModel(
                id = null,
                appSettingsId = savedAppSettingsId,
                value,
            )
        })

    }

    private suspend fun saveToDatabase(appSettingsDao: AppSettingsDao, appSettingsDbModel: AppSettingsDbModel) : Long {
        if (appSettingsDbModel.id == null) {
            return appSettingsDao.insert(appSettingsDbModel)

        }
        appSettingsDao.updateAppSettings(appSettingsDbModel)
        return appSettingsDbModel.id
    }

    override fun getContext() : Context {
        return context
    }

}
