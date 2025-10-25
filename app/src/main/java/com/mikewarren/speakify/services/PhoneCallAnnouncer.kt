package com.mikewarren.speakify.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // This makes it a singleton, accessible via Hilt
class PhoneCallAnnouncer @Inject constructor(
    private val context: Context,
    // Add other dependencies if needed, e.g., TTS instance
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

        // TODO: Get your TTS instance and speak the announcement.
        // This part needs to be connected to your existing TTS management logic.
        // For example, you might have a TTSManager that this class can use.
        Log.d("PhoneCallAnnouncer", "SPEAKING: $announcement")
    }

    @OptIn(UnstableApi::class)
    fun stopAnnouncing() {
        // TODO: Stop the TTS engine.
        Log.d("PhoneCallAnnouncer", "STOPPING announcement.")
    }
}