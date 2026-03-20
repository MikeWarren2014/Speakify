package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository

class MessengerAdditionalSettingsViewModel(
    override var settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    private val onSaveSettings: (Map<String, String>) -> Unit
) : IAdditionalSettingsViewModel {

    var includeMessageRequests by mutableStateOf(
        initialAdditionalSettings[KEY_INCLUDE_MESSAGE_REQUESTS]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    private var originalIncludeMessageRequests = includeMessageRequests

    override fun onOpen() {
        // No additional logic needed on open
    }

    override fun cancel() {
        includeMessageRequests = originalIncludeMessageRequests
    }

    override fun onSave() {
        originalIncludeMessageRequests = includeMessageRequests
        onSaveSettings(mapOf(KEY_INCLUDE_MESSAGE_REQUESTS to includeMessageRequests.toString()))
    }

    companion object {
        const val KEY_INCLUDE_MESSAGE_REQUESTS = "include_message_requests"
    }
}
