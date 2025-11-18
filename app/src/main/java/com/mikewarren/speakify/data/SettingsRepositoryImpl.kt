// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.userSettingsDataStore: DataStore<UserSettingsModel> by dataStore(
    fileName = "userSettings.pb",
    serializer = UserSettingsSerializer(),
)


class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    private val _db = DbProvider.GetDb(context)

    // Create a mutable state flow for app settings
    private val _appSettings = MutableStateFlow<Map<String, AppSettingsModel>>(emptyMap())
    override val appSettings: Flow<Map<String, AppSettingsModel>> = _appSettings.map{
        Log.d("SettingsRepositoryImpl", "Emitting: $it")
        it
    }

    init {
        loadAppSettings()
        Log.d("SettingsRepositoryImpl", "init: SettingsRepositoryImpl created")
    }

    private fun loadAppSettings() {
        // TODO: should we use the suspend fun instead?
        Log.d("SettingsRepositoryImpl", "loadAppSettings: loading settings")
        CoroutineScope(Dispatchers.IO).launch {
            _db.appSettingsDao().getAll()
                .forEach { appSettingsNestedDbModel: AppSettingsWithNotificationSources ->
                    _appSettings.update { appSettingsMap: Map<String, AppSettingsModel> ->
                        val appSettingsModel: AppSettingsModel? = AppSettingsModel.FromDbModel(appSettingsNestedDbModel)
                        if (appSettingsModel == null)
                            return@update appSettingsMap

                        return@update appSettingsMap.plus(Pair(appSettingsNestedDbModel.appSettings.packageName,
                            appSettingsModel)
                        )
                    }
                }
        }
        Log.d("SettingsRepositoryImpl", "loadAppSettings: loading finished with data: ${_appSettings.value}")
    }

    override val useDarkTheme: Flow<Boolean?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.useDarkTheme
        }

    override val selectedTTSVoice: Flow<String?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.selectedTTSVoice
        }

    override val maximizeVolumeOnScreenOff: Flow<Boolean> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.maximizeVolumeOnScreenOff
        }

    override val minVolume: Flow<Int> = context.userSettingsDataStore
        .data
        .map { model: UserSettingsModel ->
            model.minVolume
        }


    override suspend fun updateUseDarkTheme(useDarkTheme: Boolean) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(useDarkTheme = useDarkTheme)
        }
    }

    override suspend fun saveSelectedVoice(voiceName: String) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(selectedTTSVoice = voiceName)
        }
    }

    override suspend fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(maximizeVolumeOnScreenOff = shouldMaximize)
        }
    }

    override suspend fun setMinVolume(volume: Int) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(minVolume = volume)
        }
    }

    override suspend fun saveAppSettings(appSettingsModel: AppSettingsModel) {
        val appSettingsDao = _db.appSettingsDao()
        val notificationSourcesDao = _db.notificationSourcesDao()

        // create the AppSettingsWithNotificationSources object from our AppSettingsModel
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

        _appSettings.update { appSettingsMap: Map<String, AppSettingsModel> ->
            appSettingsMap.plus(Pair(appSettingsModel.packageName, appSettingsModel.copy(id = savedAppSettingsId)))
        }
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