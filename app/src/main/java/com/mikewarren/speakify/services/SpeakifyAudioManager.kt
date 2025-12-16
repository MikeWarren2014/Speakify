package com.mikewarren.speakify.services

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.mikewarren.speakify.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeakifyAudioManager @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Maximizes the music stream volume after saving the current volume.
     */
    suspend fun maximizeVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        setVolume(maxVolume)
        Log.d("SpeakifyAudioManager", "Set music volume to max: $maxVolume")
    }

    fun getVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    suspend fun setVolume(volume: Int) {
        val originalVolume = settingsRepository.originalVolume.first()
        if (originalVolume == -1) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            settingsRepository.setOriginalVolume(currentVolume)
            Log.d("SpeakifyAudioManager", "Saved original music volume: $currentVolume")
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    /**
     * Restores the music stream volume to its original state if it was saved.
     */
    suspend fun restoreVolume() {
        val originalVolume = settingsRepository.originalVolume.first()
        if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            Log.d("SpeakifyAudioManager", "Restored music volume to: $originalVolume")
        }
    }

    /**
     * Resets the saved volume state. Should be called after the user session is active again.
     */
    suspend fun resetVolumeState() {
        Log.d("SpeakifyAudioManager", "Resetting saved volume state.")
        settingsRepository.setOriginalVolume(-1)
    }

    /**
     * Checks if the phone's ringer is currently active.
     */
    fun isRingerModeNormal(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
