package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent

@AdditionalSettingsComponent(listName = "FacebookMessengerAppList")
class MessengerAdditionalSettingsViewModel(
    settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit,
) : BaseMessagingAppAdditionalSettingsViewModel(settingsRepository, initialAdditionalSettings, onSaveSettings) {


}
