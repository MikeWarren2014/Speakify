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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseTTSAutoCompletableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : BaseTTSAutoCompletableViewModel(settingsRepository) {

    val childMainVM = MainViewModel()
    val useDarkTheme: Flow<Boolean?> = settingsRepository.useDarkTheme
    var isDarkThemePreferred by mutableStateOf<Boolean?>(null)
        private set

    val maximizeVolumeOnScreenOff: StateFlow<Boolean> = settingsRepository.maximizeVolumeOnScreenOff
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Default to false
        )

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

    fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMaximizeVolumeOnScreenOff(shouldMaximize)
        }
    }
}