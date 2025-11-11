package com.mikewarren.speakify.services

import android.content.Context
import android.media.AudioManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeakifyAudioManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMusicVolume: Int = -1

    /**
     * Maximizes the music stream volume after saving the current volume.
     */
    fun maximizeVolume() {
        // Save the current volume only if it hasn't been saved yet.
        if (originalMusicVolume == -1) {
            originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            Log.d("SpeakifyAudioManager", "Saved original music volume: $originalMusicVolume")
        }
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        Log.d("SpeakifyAudioManager", "Set music volume to max: $maxVolume")
    }

    /**
     * Restores the music stream volume to its original state if it was saved.
     */
    fun restoreVolume() {
        if (originalMusicVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
            Log.d("SpeakifyAudioManager", "Restored music volume to: $originalMusicVolume")
        }
    }

    /**
     * Resets the saved volume state. Should be called after the user session is active again.
     */
    fun resetVolumeState() {
        Log.d("SpeakifyAudioManager", "Resetting saved volume state.")
        originalMusicVolume = -1
    }

    /**
     * Checks if the phone's ringer is currently active.
     */
    fun isRingerModeNormal(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}