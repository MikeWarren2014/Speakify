package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.appSettingsKeys.PhoneAppKeys

class CallingAppAdditionalSettingsViewModel(
    override var settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit,
) : BaseAppAdditionalSettingsViewModel(settingsRepository, initialAdditionalSettings, onSaveSettings) {
    var announceAnonymousCalls by mutableStateOf(
        initialAdditionalSettings[PhoneAppKeys.KEY_CALL_ANONYMOUS]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    private var originalAnnounceAnonymousCalls = announceAnonymousCalls

    override fun makeAdditionalSettingsDict(): Map<String, String> {
        return mapOf(
            PhoneAppKeys.KEY_CALL_ANONYMOUS to announceAnonymousCalls.toString(),
        )
    }

    override fun onOpen() {
        // No additional logic needed on open, yet...
    }

    override fun cancel() {
        announceAnonymousCalls = originalAnnounceAnonymousCalls
    }

}