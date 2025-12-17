package com.mikewarren.speakify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.services.SpeakifyAudioManager
import com.mikewarren.speakify.utils.log.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenStateReceiver: BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var audioManager: SpeakifyAudioManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        applicationScope.launch {
            try {
                // Use .first() to get the current value and continue. 
                // .collect() would hang indefinitely, preventing pendingResult.finish().
                val isEnabled = settingsRepository.maximizeVolumeOnScreenOff.first()
                
                if (!isEnabled) {
                    Log.d("ScreenStateReceiver", "Maximize volume is disabled.")
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
}
