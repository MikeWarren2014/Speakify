package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseTTSAutoCompletableViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnnouncerVoiceSectionViewModel(
    override var settingsRepository: SettingsRepository,
    private var initialVoice: String,
    val onSave: (String) -> Unit,
) : IAppSettingsSectionViewModel,
    BaseTTSAutoCompletableViewModel(settingsRepository) {

    var _selectedVoice = MutableStateFlow(initialVoice)
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    init {
        initializeTTS()
    }

    fun onOpen() {
        searchText = initialVoice
    }

    override fun onSelectedVoice(voiceName: String) {
        viewModelScope.launch {
            searchText = voiceName
            _selectedVoice.update { voiceName }
        }
    }

    override fun cancel() {
        viewModelScope.launch {
            _selectedVoice.update { initialVoice }
        }
    }

    override fun onSave() {
        initialVoice = selectedVoice.value
        onSave(selectedVoice.value)
    }

    override fun getTTSFlow(): Flow<String?> {
        return selectedVoice
    }
}