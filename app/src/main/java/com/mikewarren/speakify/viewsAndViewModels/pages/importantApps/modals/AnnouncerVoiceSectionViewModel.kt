package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseTTSAutoCompletableViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AnnouncerVoiceSectionViewModel(
    override var settingsRepository: SettingsRepository,
    override var initialSettings: AppSettingsModel,
    override var _settings: MutableStateFlow<AppSettingsModel>,
    override var settings: StateFlow<AppSettingsModel>,
) : IAppSettingsSectionViewModel,
    BaseTTSAutoCompletableViewModel(settingsRepository) {

    init {
        initializeTTS()
    }

    override fun saveSelectedVoice(voiceName: String) {
        viewModelScope.launch {
            val updatedSettings = _settings.value.copy(announcerVoice = voiceName)
            _settings.value = updatedSettings
            settingsRepository.saveAppSettings(updatedSettings)
        }
    }

    override fun getTTSFlow(): Flow<String?> {
        return settingsRepository.appSettings
            .map { appSettingsMap : Map<String, AppSettingsModel> ->
                val defaultTTSVoice = "en-US-language"

                appSettingsMap[initialSettings.packageName]?.announcerVoice ?: defaultTTSVoice
            }
    }
}