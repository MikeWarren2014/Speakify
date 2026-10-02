package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.constants.appSettingsKeys.MessagingAppKeys
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent

@AdditionalSettingsComponent(listName = "MessagingAppList")
@AdditionalSettingsComponent(packageName = PackageNames.GoogleVoice)
open class BaseMessagingAppAdditionalSettingsViewModel(
    settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit
) : BaseMessageReadingAdditionalSettingsViewModel(settingsRepository, initialAdditionalSettings, onSaveSettings) {

    var ignoreReactions by mutableStateOf(
        initialAdditionalSettings[MessagingAppKeys.KEY_IGNORE_REACTIONS]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    private var originalIgnoreReactions = ignoreReactions

    override fun cancel() {
        super.cancel()
        ignoreReactions = originalIgnoreReactions
    }

    override fun makeAdditionalSettingsDict(): Map<String, String> {
        val baseDict = super.makeAdditionalSettingsDict().toMutableMap()
        baseDict[MessagingAppKeys.KEY_IGNORE_REACTIONS] = ignoreReactions.toString()
        return baseDict
    }
}
