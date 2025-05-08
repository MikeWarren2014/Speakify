package com.mikewarren.speakify.viewsAndViewModels.pages

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseTTSAutoCompletableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : BaseTTSAutoCompletableViewModel(settingsRepository) {

    val useDarkTheme: Flow<Boolean?> = settingsRepository.useDarkTheme
    var isDarkThemePreferred by mutableStateOf<Boolean?>(null)
        private set
    

    init {
        observeThemePreference()
        initializeTTS()
    }

    override fun getTTSFlow(): Flow<String?> {
        return settingsRepository.selectedTTSVoice
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


    override fun onSelectedVoice(voiceName: String) {
        viewModelScope.launch {
            searchText = voiceName
            settingsRepository.saveSelectedVoice(voiceName)
        }
    }
}