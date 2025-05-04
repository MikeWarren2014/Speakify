package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.data.SettingsRepository

interface IAppSettingsSectionViewModel {
    var settingsRepository: SettingsRepository // only here because it provides Context

    abstract fun onOpen()

    abstract fun cancel()
    abstract fun onSave()

}