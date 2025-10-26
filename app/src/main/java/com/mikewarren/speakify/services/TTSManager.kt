package com.mikewarren.speakify.services

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.utils.TTSUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingAnnouncement: String? = null

    init {
        // Start initializing the TTS engine as soon as the manager is created.
        tts = TextToSpeech(context, this)
        Log.d("TTSManager", "TTS engine initialization started.")
    }

    /**
     * This callback is fired when the TTS engine is ready.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d("TTSManager", "TTS engine successfully initialized.")
            // If there was a pending announcement, speak it now.
            pendingAnnouncement?.let {
                speak(it)
                pendingAnnouncement = null
            }

            return
        }
        isInitialized = false
        Log.e("TTSManager", "TTS engine failed to initialize with status: $status")
    }

    fun speak(text: String, voiceName: String? = null) {
        if (isInitialized) {
            TTSUtils.SetTTSVoice(tts, voiceName)

            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)

            return
        }
        // Engine isn't ready yet, save the announcement to be spoken once it is.
        pendingAnnouncement = text
        Log.d("TTSManager", "TTS not ready. Queuing announcement.")
    }

    fun stop() {
        tts?.stop()
    }

    // You might call this from your Application's onDestroy if needed,
    // but as a singleton, it will live for the process lifetime.
    fun shutdown() {
        tts?.shutdown()
        isInitialized = false
    }
}
