package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel
import kotlinx.coroutines.flow.last

class ConfigurableAppListItemViewModel(
    override val model: UserAppModel,
    private val settingsRepository: SettingsRepository):
    AppListItemViewModel(model){
    val childViewModel: AppSettingsViewModel = AppSettingsViewModel(
        appModel = model,
        initialSettings = AppSettingsModel(
            packageName = model.packageName,
            announcerVoice = "",
            notificationSources = emptyList(),
        ),
        settingsRepository = settingsRepository,
    )
}