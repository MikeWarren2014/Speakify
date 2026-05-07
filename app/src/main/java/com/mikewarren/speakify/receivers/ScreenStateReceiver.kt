package com.mikewarren.speakify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.services.SpeakifyAudioManager
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.log.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenStateReceiver @Inject constructor(): BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var audioManager: SpeakifyAudioManager

    @Inject
    lateinit var ttsManager: TTSManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        applicationScope.launch {
            try {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    val stopSpeechOnScreenOff = settingsRepository.stopSpeechOnScreenOff.first()
                    if (stopSpeechOnScreenOff) {
                        stopSpeakificationIfNeeded()
                        return@launch
                    }

                    val maximizeVolumeOnScreenOff = settingsRepository.maximizeVolumeOnScreenOff.first()
                    if (maximizeVolumeOnScreenOff) {
                        Log.d("ScreenStateReceiver", "Screen OFF. Maximizing notification volume.")
                        audioManager.maximizeVolume()
                    }
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

                Log.d(
                    "ScreenStateReceiver",
                    "User PRESENT. Restoring original notification volume."
                )
                // Restore as a fallback and, more importantly, reset the state.
                audioManager.restoreVolume()
                audioManager.resetVolumeState()
            } catch (e: Exception) {
                LogUtils.LogNonFatalError("ScreenStateReceiver", "Error processing screen state change.", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    suspend fun stopSpeakificationIfNeeded() {
        if (ttsManager.isSpeakificationInProgress()) {
            Log.d("ScreenStateReceiver", "Screen OFF. Stopping TTS.")
            ttsManager.stop()
        }

    }
}
