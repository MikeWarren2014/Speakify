package com.mikewarren.speakify.viewsAndViewModels.pages

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.BackupRepository
import com.mikewarren.speakify.data.SessionRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseTTSAutoCompletableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    ttsManager: TTSManager,
    private val backupRepository: BackupRepository,
    private val sessionRepository: SessionRepository,
) : BaseTTSAutoCompletableViewModel(settingsRepository, ttsManager) {

    val childMainVM = MainViewModel(sessionRepository)
    val useDarkTheme: Flow<Boolean?> = settingsRepository.useDarkTheme
    var isDarkThemePreferred by mutableStateOf<Boolean?>(null)
        private set

    val maximizeVolumeOnScreenOff: StateFlow<Boolean> = settingsRepository.maximizeVolumeOnScreenOff
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Default to false
        )

    val minVolume: StateFlow<Int> = settingsRepository.minVolume
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0 // Default to 0
        )

    val isCrashlyticsEnabled: StateFlow<Boolean> = settingsRepository.isCrashlyticsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Default to false
        )

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeThemePreference()
        observeVoicePreference()
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

    fun setMinVolume(volume: Int) {
        viewModelScope.launch {
            settingsRepository.setMinVolume(volume)
        }
    }

    fun setCrashlyticsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrashlyticsEnabled(isEnabled)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("Exporting backup..."))
            // 2. Perform the export
            val result = backupRepository.exportData(uri)
            // 3. Emit events based on result
            if (result.isSuccess) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Backup exported successfully!"))
            } else {
                _uiEvent.emit(UiEvent.ShowSnackbar("Export failed: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("Restoring data..."))
            val result = backupRepository.importData(uri)
            if (result.isSuccess) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Data restored successfully!"))
            } else {
                _uiEvent.emit(UiEvent.ShowSnackbar("Import failed: ${result.exceptionOrNull()?.message}"))
            }
        }
    }
}
