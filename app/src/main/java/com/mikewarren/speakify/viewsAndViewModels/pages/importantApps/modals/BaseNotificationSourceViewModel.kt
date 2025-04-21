package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseAutoCompletableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseNotificationSourceViewModel(
    override var settingsRepository: SettingsRepository,
    override var initialSettings: AppSettingsModel,
    override var _settings: MutableStateFlow<AppSettingsModel>,
    override var settings: StateFlow<AppSettingsModel>,
) :
    IAppSettingsSectionViewModel, BaseAutoCompletableViewModel() {
    open override fun getLabel(): String {
        return "Notification Source"
    }
}