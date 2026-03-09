package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.SettingsRepository

class MessengerAdditionalSettingsViewModel(
    override var settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    private val onSaveSettings: (Map<String, String>) -> Unit
) : IAdditionalSettingsViewModel {

    var ignoreMessageRequests by mutableStateOf(
        initialAdditionalSettings[KEY_IGNORE_MESSAGE_REQUESTS]?.toBoolean() ?: true
    )

    private var originalIgnoreMessageRequests = ignoreMessageRequests

    override fun onOpen() {
        // No additional logic needed on open
    }

    override fun cancel() {
        ignoreMessageRequests = originalIgnoreMessageRequests
    }

    override fun onSave() {
        originalIgnoreMessageRequests = ignoreMessageRequests
        onSaveSettings(mapOf(KEY_IGNORE_MESSAGE_REQUESTS to ignoreMessageRequests.toString()))
    }

    companion object {
        const val KEY_IGNORE_MESSAGE_REQUESTS = "ignore_message_requests"
    }
}
