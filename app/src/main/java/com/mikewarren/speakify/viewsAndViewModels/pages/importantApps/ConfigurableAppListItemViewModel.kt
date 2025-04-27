package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AppSettingsViewModel

class ConfigurableAppListItemViewModel(
    override val model: UserAppModel,
    private val settingsRepository: SettingsRepository):
    AppListItemViewModel(model){
    var childViewModel: AppSettingsViewModel = AppSettingsViewModel(
        appModel = model,
        initialSettingsModel = AppSettingsModel(
            packageName = model.packageName,
            announcerVoice = Constants.DefaultTTSLanguage,
            notificationSources = emptyList(),
        ),
        settingsRepository = settingsRepository,
    )

}