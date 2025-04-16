// SettingsRepository.kt
package com.mikewarren.speakify.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val useDarkTheme: Flow<Boolean?>
    val selectedTTSVoice: Flow<String?>
    suspend fun updateUseDarkTheme(useDarkTheme: Boolean)
    suspend fun saveSelectedVoice(voiceName: String)

    fun getContext() : Context
}