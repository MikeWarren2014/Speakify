package com.mikewarren.speakify.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // This makes it a singleton, accessible via Hilt
class PhoneCallAnnouncer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsManager: TTSManager
) {
    @OptIn(UnstableApi::class)
    fun announceCall(phoneNumber: String) {
        // Here, you'd resolve the contact name from the phone number
        val contactName = NotificationExtractionUtils.GetDisplayNameForPhoneNumber(context, phoneNumber)

        val announcement = if (contactName.isNotEmpty()) {
            "Incoming call from $contactName"
        } else {
            "Incoming call from ${phoneNumber.toCharArray().joinToString(" ")}" // Spells out the number
        }

        Log.d("PhoneCallAnnouncer", "SPEAKING: $announcement")
        ttsManager.speak(announcement)
    }

    @OptIn(UnstableApi::class)
    fun stopAnnouncing() {
        Log.d("PhoneCallAnnouncer", "STOPPING announcement.")
        ttsManager.stop()
    }
}