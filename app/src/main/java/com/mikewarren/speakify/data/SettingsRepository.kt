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

    suspend fun updateUseDarkTheme(useDarkTheme: Boolean)
    suspend fun saveSelectedVoice(voiceName: String)
    suspend fun saveAppSettings(appSettingsModel: AppSettingsModel)

    suspend fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean)

    suspend fun setMinVolume(volume: Int)

    fun getContext() : Context

    // TODO: this shouldn't be here in SettingsRepository, but right now I can't think of a better place to put it
    val hasRequestedPhonePermissions: Flow<Boolean>

    suspend fun setPhonePermissionsRequested(hasRequested: Boolean)
}