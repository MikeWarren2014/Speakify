package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import com.mikewarren.speakify.data.AppSettingsWithNotificationSources
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AppSettingsViewModel

class ConfigurableAppListItemViewModel(
    override val model: UserAppModel,
    private val settingsRepository: SettingsRepository,
):
    AppListItemViewModel(model){
    var childViewModel: AppSettingsViewModel = AppSettingsViewModel(
        appModel = model,
        initialSettingsModel = AppSettingsModel(
            id = null,
            packageName = model.packageName,
            announcerVoice = Constants.DefaultTTSVoice,
            notificationSources = emptyList(),
        ),
        settingsRepository = settingsRepository,
    )

}