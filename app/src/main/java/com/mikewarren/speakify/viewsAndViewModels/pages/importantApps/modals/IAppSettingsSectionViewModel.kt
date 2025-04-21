package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IAppSettingsSectionViewModel {
    var settingsRepository: SettingsRepository
    var initialSettings: AppSettingsModel

    var _settings: MutableStateFlow<AppSettingsModel>
    var settings: StateFlow<AppSettingsModel>

}