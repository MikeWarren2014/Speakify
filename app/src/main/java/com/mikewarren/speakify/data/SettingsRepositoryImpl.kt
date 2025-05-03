// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.userSettingsDataStore: DataStore<UserSettingsModel> by dataStore(
    fileName = "userSettings.pb",
    serializer = UserSettingsSerializer(),
)


class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val useDarkTheme: Flow<Boolean?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.useDarkTheme
        }

    override val selectedTTSVoice: Flow<String?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.selectedTTSVoice
        }

    // Create a mutable state flow for app settings
    private val _appSettings = MutableStateFlow<Map<String, AppSettingsModel>>(emptyMap())
//    override val appSettings: Flow<Map<String, AppSettingsModel>> = context.userSettingsDataStore.data
//        .map { model: UserSettingsModel ->
//            Log.d("SettingsRepositoryImpl", "Emitting: ${model.appSettings}")
//            model.appSettings
//        }
    override val appSettings: Flow<Map<String, AppSettingsModel>> = _appSettings.map{
        Log.d("SettingsRepositoryImpl", "Emitting: $it")
        it
    }

    init {
        loadAppSettings()
        Log.d("SettingsRepositoryImpl", "init: SettingsRepositoryImpl created")
    }

    private fun loadAppSettings() {
        Log.d("SettingsRepositoryImpl", "loadAppSettings: loading settings")
        CoroutineScope(Dispatchers.IO).launch {
            context.userSettingsDataStore.data.collect { model: UserSettingsModel ->
                Log.d("SettingsRepositoryImpl", "loadAppSettings - collect: DataStore emitted: $model")
                _appSettings.update {
                    Log.d("SettingsRepositoryImpl", "loadAppSettings - _appSettings.update: Updating _appSettings with ${model.appSettings}")
                    model.appSettings
                }
            }
        }
        Log.d("SettingsRepositoryImpl", "loadAppSettings: loading finished")
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

    override suspend fun saveAppSettings(appSettingsModel: AppSettingsModel) {
//        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
//            val currentAppSettings = model.appSettings.toMutableMap()
//            currentAppSettings[appSettingsModel.packageName] = appSettingsModel
//            model.copy(appSettings = currentAppSettings)
//        }
        Log.d("SettingsRepositoryImpl", "saveAppSettings: Saving app settings: $appSettings")
        // Get current map
        val userSettingsDataStore = context.userSettingsDataStore

        // Get current map
        var currentMap = userSettingsDataStore.data.map { model -> model.appSettings }.first()
        // Add new data
        currentMap = currentMap.plus(Pair(appSettingsModel.packageName, appSettingsModel))
        // Save map
        userSettingsDataStore.updateData { userSettingsModel: UserSettingsModel ->
            Log.d("SettingsRepositoryImpl", "saveAppSettings - updateData: Updating DataStore with: $userSettingsModel")
            userSettingsModel.copy(appSettings = currentMap)
        }
    }

    override fun getContext() : Context {
        return context
    }
}