package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.appSettingsKeys.EmailAppKeys
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent

@AdditionalSettingsComponent(appTypes = ["email"])
class EmailAppAdditionalSettingsViewModel(
    settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit,
) : BaseMessageReadingAdditionalSettingsViewModel(
    settingsRepository,
    initialAdditionalSettings,
    onSaveSettings
) {
    var announceMultipleMessages by mutableStateOf(
        initialAdditionalSettings[EmailAppKeys.KEY_ANNOUNCE_MULTIPLE_MESSAGE]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    var messageKeywords by mutableStateOf(
        initialAdditionalSettings[EmailAppKeys.KEY_MESSAGE_KEYWORDS] ?: ""
    )

    private var originalAnnounceMultipleMessages = announceMultipleMessages
    private var originalMessageKeywords = messageKeywords

    override fun cancel() {
        super.cancel()
        announceMultipleMessages = originalAnnounceMultipleMessages
        messageKeywords = originalMessageKeywords
    }

    override fun makeAdditionalSettingsDict(): Map<String, String> {
        val baseDict = super.makeAdditionalSettingsDict().toMutableMap()
        baseDict[EmailAppKeys.KEY_ANNOUNCE_MULTIPLE_MESSAGE] = announceMultipleMessages.toString()
        baseDict[EmailAppKeys.KEY_MESSAGE_KEYWORDS] = messageKeywords
        return baseDict
    }
}
