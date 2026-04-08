package com.mikewarren.speakify.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.models.VoiceInfoModel
import com.mikewarren.speakify.utils.TTSUtils
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val audioManager: SpeakifyAudioManager,
) : ITaggable,
    TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isInitializationStarted = false
    private val initializationLock = Any()

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
            Handler(Looper.getMainLooper()).post {
                try {
                    tts = TextToSpeech(context, this)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to instantiate TextToSpeech", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    isInitializationStarted = false
                }
            }
            isInitializationStarted = true
            Log.d(TAG, "TTS engine initialization queued.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d(TAG, "TTS engine successfully initialized.")
            return
        }
        isInitialized = false
        isInitializationStarted = false
        Log.e(TAG, "TTS engine failed to initialize with status: $status")
        FirebaseCrashlytics.getInstance().log("TTS engine failed to initialize with status: $status")
    }

    /**
     * Speaks the given text using the TTS engine.
     * @return true if the text was spoken successfully, false otherwise.
     */
    suspend fun speak(text: String, voiceName: String? = null): Boolean {
        initialize()

        if (!isInitialized) {
            Log.d(TAG, "TTS not ready. Waiting for initialization...")
            try {
                withTimeout(5000) {
                    while (!isInitialized) {
                        delay(100)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Timed out waiting for TTS initialization.")
                isInitializationStarted = false
                return false
            }
        }

        val minVolume = settingsRepository.minVolume.first()
        val currentVolume = audioManager.getVolume()
        if (currentVolume < minVolume) {
            audioManager.setVolume(minVolume, force = true)
        }
        
        audioManager.requestAudioFocus()

        return try {
            suspendCancellableCoroutine { continuation ->
                val utteranceId = UUID.randomUUID().toString()

                // If the coroutine is cancelled while speaking, we stop the TTS engine.
                // tts.stop() is a synchronous call and safe to use here.
                continuation.invokeOnCancellation {
                    tts?.stop()
                }

                tts?.setOnUtteranceProgressListener(UtteranceListener(utteranceId, continuation, TAG))
                TTSUtils.SetTTSVoice(tts, voiceName)

                val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)

                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS engine returned ERROR for text: $text")
                    FirebaseCrashlytics.getInstance().log("TTS engine returned ERROR for text: $text")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            // Rethrow cancellation exceptions so they aren't logged as errors in Crashlytics
            if (e is CancellationException) throw e

            LogUtils.LogNonFatalError(TAG, "An error occurred while speaking", e)
            false
        } finally {
            // Since restoreVolume is a suspend function, we must use withContext(NonCancellable)
            // to ensure it can execute even when the parent coroutine is cancelled.
            withContext(NonCancellable) {
                audioManager.abandonAudioFocus()
                audioManager.restoreVolume()
            }
        }
    }

    fun getRecommendedDefaultVoiceModels(): List<VoiceInfoModel> {
        if (isInitialized)
            return TTSUtils.GetRecommendedDefaultVoiceModels(tts!!)
        return emptyList()
    }

    /**
     * Retrieves a structured list of all available TTS voices with user-friendly display names.
     * @return A list of VoiceInfoModel objects, or an empty list if the TTS engine is not ready.
     */
    fun getVoiceInfoList(): List<VoiceInfoModel> {
        if (!isInitialized || tts?.voices == null) {
            Log.w(TAG, "getVoiceInfoList called but TTS is not initialized or has no voices.")
            return emptyList()
        }

        return tts!!.voices.mapNotNull { voice ->
            TTSUtils.ToVoiceInfoModel(voice)
        }
    }

    fun setVoice(voiceName: String? = Constants.DefaultTTSVoice) {
        if (!isInitialized)
            return

        TTSUtils.SetTTSVoice(tts!!, voiceName)
    }

    suspend fun stop() {
        Log.d(TAG, "Force stopping TTS engine.")
        // Force flush the queue with an empty string to clear any hardware buffers
        tts?.speak("", TextToSpeech.QUEUE_FLUSH, null, "stop_utterance")
        tts?.stop()
        audioManager.abandonAudioFocus()
        audioManager.restoreVolume()
    }

    fun shutdown() {
        tts?.shutdown()
        isInitialized = false
    }
}
