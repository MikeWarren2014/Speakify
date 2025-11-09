// SettingsRepository.kt
package com.mikewarren.speakify.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val useDarkTheme: Flow<Boolean?>
    val selectedTTSVoice: Flow<String?>
    val appSettings: Flow<Map<String, AppSettingsModel>>

    val maximizeVolumeOnScreenOff: Flow<Boolean>

    suspend fun updateUseDarkTheme(useDarkTheme: Boolean)
    suspend fun saveSelectedVoice(voiceName: String)
    suspend fun saveAppSettings(appSettingsModel: AppSettingsModel)

    suspend fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean)

    fun getContext() : Context
}