package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.appSettingsKeys.MessagingAppKeys

abstract class BaseMessagingAppAdditionalSettingsViewModel(
    settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit
) : BaseAppAdditionalSettingsViewModel(settingsRepository, initialAdditionalSettings, onSaveSettings) {
    var readMessages by mutableStateOf(
        initialAdditionalSettings[MessagingAppKeys.KEY_READ_MESSAGES]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    var ignoreSingleWordMessages by mutableStateOf(
        initialAdditionalSettings[MessagingAppKeys.KEY_IGNORE_SINGLE_WORD_MESSAGES]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    var ignoreReactions by mutableStateOf(
        initialAdditionalSettings[MessagingAppKeys.KEY_IGNORE_REACTIONS]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )


    override fun onOpen() {
        // No additional logic needed on open, yet...
    }

    override fun makeAdditionalSettingsDict(): Map<String, String> {
        return mapOf(
            MessagingAppKeys.KEY_READ_MESSAGES to readMessages.toString(),
            MessagingAppKeys.KEY_IGNORE_SINGLE_WORD_MESSAGES to ignoreSingleWordMessages.toString(),
            MessagingAppKeys.KEY_IGNORE_REACTIONS to ignoreReactions.toString(),
        )
    }

}