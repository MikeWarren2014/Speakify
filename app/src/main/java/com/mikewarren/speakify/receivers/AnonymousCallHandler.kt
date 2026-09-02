package com.mikewarren.speakify.receivers

import android.content.Context
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.appSettingsKeys.PhoneAppKeys
import com.mikewarren.speakify.services.PhoneCallAnnouncer
import com.mikewarren.speakify.services.SpeakifyEngineGatekeeper

interface AnonymousCallHandler {
    val context: Context
    val announcer: PhoneCallAnnouncer
    val gatekeeper: SpeakifyEngineGatekeeper

    val appSettingsModel: AppSettingsModel?

    suspend fun handleAnonymousCall() {
        if (!gatekeeper.canSpeakNow()) {
            return
        }

        if (appSettingsModel
            ?.additionalSettings[PhoneAppKeys.KEY_CALL_ANONYMOUS]
            ?.toBoolean() == true)
            announcer.announceCall(context.getString(R.string.anonymous_caller))

    }
}