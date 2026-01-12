// SettingsRepository.kt
package com.mikewarren.speakify.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {


    val useDarkTheme: Flow<Boolean?>
    val selectedTTSVoice: Flow<String?>
    val appSettings: Flow<Map<String, AppSettingsModel>>

    val maximizeVolumeOnScreenOff: Flow<Boolean>

    val minVolume: Flow<Int>

    val isCrashlyticsEnabled: Flow<Boolean>
    val originalVolume: Flow<Int> // Persisted original volume

    suspend fun updateUseDarkTheme(useDarkTheme: Boolean)
    suspend fun saveSelectedVoice(voiceName: String)
    suspend fun saveAppSettings(appSettingsModel: AppSettingsModel)

    suspend fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean)

    suspend fun setMinVolume(volume: Int)

    suspend fun setCrashlyticsEnabled(isEnabled: Boolean)
    suspend fun setOriginalVolume(volume: Int) // Save original volume

    fun getContext() : Context

}
