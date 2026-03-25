package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.appSettingsKeys.MessagingAppKeys

class MessengerAdditionalSettingsViewModel(
    override var settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit,
) : BaseMessagingAppAdditionalSettingsViewModel(settingsRepository, initialAdditionalSettings, onSaveSettings) {

    var includeMessageRequests by mutableStateOf(
        initialAdditionalSettings[MessagingAppKeys.KEY_INCLUDE_MESSAGE_REQUESTS]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    private var originalIncludeMessageRequests = includeMessageRequests

    override fun cancel() {
        includeMessageRequests = originalIncludeMessageRequests
    }

    override fun onSave() {
        originalIncludeMessageRequests = includeMessageRequests
        super.onSave()
    }

    override fun makeAdditionalSettingsDict(): Map<String, String> {
        val baseMap = super.makeAdditionalSettingsDict().toMutableMap()
        baseMap[MessagingAppKeys.KEY_INCLUDE_MESSAGE_REQUESTS] = includeMessageRequests.toString()
        return baseMap
    }

}
