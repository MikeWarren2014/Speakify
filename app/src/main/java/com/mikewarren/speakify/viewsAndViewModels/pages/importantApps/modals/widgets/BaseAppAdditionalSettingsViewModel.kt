package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.IAppSettingsSectionViewModel

abstract class BaseAppAdditionalSettingsViewModel(
    settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    protected val onSaveSettings: (Map<String, String>) -> Unit
) : IAppSettingsSectionViewModel {
    override fun onSave() {
        onSaveSettings(makeAdditionalSettingsDict())
    }

    abstract fun makeAdditionalSettingsDict(): Map<String, String>
}