package com.mikewarren.speakify.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.utils.log.LogUtils
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // This makes it a singleton, accessible via Hilt
class PhoneCallAnnouncer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsManager: TTSManager
) {
    private val announcerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var announcementJob: Job? = null

    @OptIn(UnstableApi::class)
    suspend fun announceCall(phoneNumber: String) {
        // Stop any previous announcement loops
        stopAnnouncing()

        val contactName = NotificationExtractionUtils.GetDisplayNameForPhoneNumber(context, phoneNumber)

        val announcement = if (contactName.isNotEmpty()) {
            "Incoming call from $contactName"
        } else {
            "Incoming call from ${phoneNumber.toCharArray().joinToString(" ")}" // Spells out the number
        }

        announcementJob = announcerScope.launch {
            try {
                withTimeout(25 * Constants.OneSecond) {
                    while (isActive) {
                        Log.d("PhoneCallAnnouncer", "SPEAKING: $announcement")
                        ttsManager.speak(announcement) // This should be a suspend function
                        delay(500) // Wait half a second after speech finishes
                    }
                }
            } catch (e: TimeoutCancellationException) {
                LogUtils.LogNonFatalError("PhoneCallAnnouncer", "Phone call announcement exceeded 25 seconds limit!", e)
            }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun stopAnnouncing() {
        if (announcementJob?.isActive == true) {
            Log.d("PhoneCallAnnouncer", "STOPPING announcement loop.")
            announcementJob?.cancel()
        }
        announcementJob = null
        ttsManager.stop()
    }
}
