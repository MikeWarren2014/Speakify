package com.mikewarren.speakify.services

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.utils.TTSUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val audioManager: SpeakifyAudioManager,
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isInitializationStarted = false
    private val initializationLock = Any()

    // NEW: A simple way to proactively start init
    init {
        initialize()
    }

    private fun initialize() {
        if (isInitializationStarted) {
            return
        }
        synchronized(initializationLock) {
            if (isInitializationStarted) {
                return
            }
            // The TextToSpeech constructor must be called on a thread with a Looper.
            Handler(Looper.getMainLooper()).post {
                try {
                    tts = TextToSpeech(context, this)
                } catch (e: Exception) {
                    Log.e("TTSManager", "Failed to instantiate TextToSpeech", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    isInitializationStarted = false // Allow retry
                }
            }
            isInitializationStarted = true
            Log.d("TTSManager", "TTS engine initialization queued.")
        }
    }

    /**
     * This callback is fired when the TTS engine is ready.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d("TTSManager", "TTS engine successfully initialized.")
            return
        }
        isInitialized = false
        isInitializationStarted = false // Reset so we can try again next time
        Log.e("TTSManager", "TTS engine failed to initialize with status: $status")
        FirebaseCrashlytics.getInstance().log("TTS engine failed to initialize with status: $status")
    }

    suspend fun speak(text: String, voiceName: String? = null) {
        // 1. Ensure we are initialized or initializing
        initialize()

        // 2. Wait for initialization (with a timeout to prevent infinite hanging)
        if (!isInitialized) {
            Log.d("TTSManager", "TTS not ready. Waiting for initialization...")
            try {
                withTimeout(5000) { // Wait up to 5 seconds
                    while (!isInitialized) {
                        delay(100) // Check every 100ms
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("TTSManager", "Timed out waiting for TTS initialization.")
                FirebaseCrashlytics.getInstance().log("Timed out waiting for TTS initialization.")
                return // Give up if it takes too long
            }
        }

        // 3. Proceed with normal speaking logic
        val minVolume = settingsRepository.minVolume.first()

        val currentVolume = audioManager.getVolume()
        if (currentVolume < minVolume) {
            audioManager.setVolume(minVolume)
        }
        try {

            return suspendCancellableCoroutine { continuation ->
                val utteranceId = UUID.randomUUID().toString()

                val listener = object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}

                    override fun onDone(id: String?) {
                        if (id == utteranceId && continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    @Deprecated("deprecated in API level 21")
                    override fun onError(id: String?) {
                        if (id == utteranceId && continuation.isActive) {
                            continuation.resumeWithException(RuntimeException("TTS error"))
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId && continuation.isActive) {
                            continuation.resumeWithException(RuntimeException("TTS error with code $errorCode"))
                        }
                    }
                }

                // Set listener specifically for this utterance if possible,
                // but standard API sets it globally.
                tts?.setOnUtteranceProgressListener(listener)

                TTSUtils.SetTTSVoice(tts, voiceName)

                val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)

                if (result == TextToSpeech.ERROR) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(RuntimeException("TTS engine returned ERROR"))
                    }
                }
            }
        } finally {
            audioManager.restoreVolume()
        }
    }

    fun getRecommendedDefaultVoiceNames(): List<String> {
        if (isInitialized)
            return TTSUtils.GetRecommendedDefaultVoiceNames(tts!!)

        return emptyList()
    }

    fun getAllVoiceNames(): List<String> {
        if (isInitialized)
            return tts!!.voices.map { it.name }

        return emptyList()
    }

    fun setVoice(voiceName: String? = Constants.DefaultTTSVoice) {
        if (!isInitialized)
            return

        TTSUtils.SetTTSVoice(tts!!, voiceName)
    }


    suspend fun stop() {
        tts?.stop()
        audioManager.restoreVolume()
    }

    fun shutdown() {
        tts?.shutdown()
        isInitialized = false
    }
}