package com.mikewarren.speakify.viewsAndViewModels.pages

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    // No longer needs Context directly
) : ViewModel() {
    // Text-to-speech settings
    var tts: TextToSpeech? = null
    val useDarkTheme: Flow<Boolean?> = settingsRepository.useDarkTheme
    var isDarkThemePreferred by mutableStateOf<Boolean?>(null)
        private set
    val selectedTTSVoice: Flow<String?> = settingsRepository.selectedTTSVoice
    init {
        observeThemePreference()
        initializeTTS()
        observeVoicePreference()
    }
    private fun observeVoicePreference() {
        viewModelScope.launch {
            settingsRepository.selectedTTSVoice.collectLatest { voiceName ->
                setTTSVoice(voiceName)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setTTSVoice(voiceName: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = tts?.voices
            if (!voices.isNullOrEmpty()) {
                // Use provided voiceName or system default if null
                val voice = voiceName?.let { name ->
                    voices.find { it.name == name }
                } ?: voices.find { it.locale == Locale.getDefault() } ?: voices.firstOrNull()

                voice?.let {
                    tts?.voice = it
                    // No need to save here; saving is done in SettingsView
                }
            }
        }
    }
    fun updateUseDarkTheme(useDarkTheme: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseDarkTheme(useDarkTheme)
        }
    }
    private fun observeThemePreference() {
        viewModelScope.launch {
            useDarkTheme.collect { isDarkTheme ->
                isDarkThemePreferred = isDarkTheme
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(settingsRepository.getContext(), { status ->
            if (status == TextToSpeech.SUCCESS) {
                setTTSVoice() // Call setTTSVoice when TTS is ready
                observeVoicePreference()
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        })
    }

    fun saveSelectedVoice(voiceName: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedVoice(voiceName)
        }
    }

    // Cleanup
    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}