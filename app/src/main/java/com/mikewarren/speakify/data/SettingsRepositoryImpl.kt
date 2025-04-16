// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepositoryImpl @Inject constructor(private val context: Context) : SettingsRepository {

    private val useDarkThemeKey = booleanPreferencesKey("use_dark_theme")
    override val useDarkTheme: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            preferences[useDarkThemeKey]
        }
    private val ttsVoiceKey = stringPreferencesKey("selected_tts_voice")
    override val selectedTTSVoice: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ttsVoiceKey]
        }
    override suspend fun updateUseDarkTheme(useDarkTheme: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useDarkThemeKey] = useDarkTheme
        }
    }

    override suspend fun saveSelectedVoice(voiceName: String) {
        context.dataStore.edit { preferences ->
            preferences[ttsVoiceKey] = voiceName
        }
    }

    override fun getContext() : Context {
        return context
    }
}