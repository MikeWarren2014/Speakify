package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    val appModel: UserAppModel,
    initialSettings: AppSettingsModel,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var isOpen by mutableStateOf(false)

    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<AppSettingsModel> = _settings

    val childAnnouncerVoiceSectionViewModel = AnnouncerVoiceSectionViewModel(
        settingsRepository = settingsRepository,
        initialSettings = initialSettings,
        _settings = _settings,
        settings = settings,
    )


    fun addNotificationSource(source: String) {
        viewModelScope.launch {
            val updatedSources = _settings.value.notificationSources + source
            val updatedSettings = _settings.value.copy(notificationSources = updatedSources)
            _settings.value = updatedSettings
            settingsRepository.saveAppSettings(updatedSettings)
        }
    }

    fun removeNotificationSource(source: String) {
        viewModelScope.launch {
            val updatedSources = _settings.value.notificationSources - source
            val updatedSettings = _settings.value.copy(notificationSources = updatedSources)
            _settings.value = updatedSettings
            settingsRepository.saveAppSettings(updatedSettings)
        }
    }
}