package com.mikewarren.speakify.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
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
    private var focusRequest: AudioFocusRequest? = null

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    /**
     * Maximizes the music stream volume after saving the current volume.
     */
    suspend fun maximizeVolume() {
        setVolume(maxVolume, force = true)
        Log.d("SpeakifyAudioManager", "Set music volume to max: $maxVolume")
    }

    fun getVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    suspend fun setVolume(volume: Int, force: Boolean = false) {
        if (!force && isMusicActive()) {
            Log.d("SpeakifyAudioManager", "Aborted setting volume because other audio is playing.")
            return
        }

        val originalVolume = settingsRepository.originalVolume.first()
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if ((originalVolume == -1) || (originalVolume != currentVolume)) {
            settingsRepository.setOriginalVolume(currentVolume)
            Log.d("SpeakifyAudioManager", "Saved original music volume: $currentVolume")
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    /**
     * Requests audio focus to duck other apps while speaking.
     */
    fun requestAudioFocus(): Int {
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                Log.d("SpeakifyAudioManager", "Audio focus changed: $focusChange")
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest!!)
        Log.d("SpeakifyAudioManager", "Audio focus request result: $result")
        return result
    }

    /**
     * Abandons audio focus, allowing other apps to resume normal volume.
     */
    fun abandonAudioFocus() {
        focusRequest?.let {
            val result = audioManager.abandonAudioFocusRequest(it)
            Log.d("SpeakifyAudioManager", "Abandoned audio focus result: $result")
        }
        focusRequest = null
    }

    /**
     * Checks if music is actively playing on the device.
     */
    private fun isMusicActive(): Boolean {
        return audioManager.isMusicActive
    }

    /**
     * Restores the music stream volume to its original state if it was saved.
     */
    suspend fun restoreVolume() {
        val originalVolume = settingsRepository.originalVolume.first()
        val currentVolume = getVolume()
        if (originalVolume != -1 && currentVolume < originalVolume) {
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
