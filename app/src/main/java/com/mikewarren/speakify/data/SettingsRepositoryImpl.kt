// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.userSettingsDataStore: DataStore<UserSettingsModel> by dataStore(
    fileName = "userSettings.pb",
    serializer = UserSettingsSerializer(),
)


class SettingsRepositoryImpl @Inject constructor(private val context: Context) : SettingsRepository {

    override val useDarkTheme: Flow<Boolean?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.useDarkTheme
        }

    override val selectedTTSVoice: Flow<String?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.selectedTTSVoice
        }
    override val appSettings: Flow<Map<String, AppSettingsModel>> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.appSettings
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
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            val currentAppSettings = model.appSettings.toMutableMap()
            currentAppSettings[appSettingsModel.packageName] = appSettingsModel
            model.copy(appSettings = currentAppSettings)
        }
    }

    override fun getContext() : Context {
        return context
    }
}