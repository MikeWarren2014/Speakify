package com.mikewarren.speakify.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.activity.result.launch
import androidx.annotation.OptIn
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenStateReceiver: BroadcastReceiver() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var audioManager: SpeakifyAudioManager // Inject the new wrapper

    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {

        // Use goAsync to handle the asynchronous check of the setting
        val pendingResult = goAsync()

        // Launch a coroutine to safely perform the async check
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, check if the setting is enabled. Use .first() to get the current value.
                val isEnabled = settingsRepository.maximizeVolumeOnScreenOff.first()

                // If the setting is disabled, do nothing and exit the coroutine.
                if (!isEnabled) {
                    return@launch
                }
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    Log.d("ScreenStateReceiver", "Screen OFF. Maximizing notification volume.")
                    audioManager.maximizeVolume()
                    return@launch
                }

                if (intent.action == Intent.ACTION_SCREEN_ON) {
                    Log.d("ScreenStateReceiver", "Screen ON. Checking for call before restoring volume.")

                    // Check if the phone is currently ringing
                    if (!audioManager.isRingerModeNormal()) {
                        // If not ringing, it's safe to restore volume to prevent media race condition
                        Log.d("ScreenStateReceiver", "Not ringing. Restoring volume immediately.")
                        audioManager.restoreVolume()
                        return@launch
                    }
                    // Phone is ringing, so do NOT restore volume. Let it ring at max.
                    Log.d("ScreenStateReceiver", "Phone is ringing. Keeping volume maximized for the call.")
                    return@launch
                }

                if (intent.action == Intent.ACTION_USER_PRESENT) {
                    Log.d(
                        "ScreenStateReceiver",
                        "User PRESENT. Restoring original notification volume."
                    )
                    // Restore as a fallback and, more importantly, reset the state.
                    audioManager.restoreVolume()
                    audioManager.resetVolumeState()
                }
            } catch (e: Exception) {
                Log.e("ScreenStateReceiver", "Error processing screen state change.", e)
            } finally {
                // IMPORTANT: Always finish the pending result to signal completion.
                pendingResult.finish()
            }
        }
    }
}