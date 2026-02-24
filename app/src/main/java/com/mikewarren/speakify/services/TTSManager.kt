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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
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

    suspend fun speak(text: String, voiceName: String? = null) {
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
                return
            }
        }

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
                            Log.e(TAG, "TTS error for utterance: $id")
                            continuation.resume(Unit)
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId && continuation.isActive) {
                            Log.e(TAG, "TTS error with code $errorCode for utterance: $id")
                            continuation.resume(Unit)
                        }
                    }

                    override fun onStop(id: String?, interrupted: Boolean) {
                        if (id == utteranceId && continuation.isActive) {
                            Log.d(TAG, "TTS utterance stopped: $id")
                            continuation.resume(Unit)
                        }
                    }
                }

                tts?.setOnUtteranceProgressListener(listener)
                TTSUtils.SetTTSVoice(tts, voiceName)

                val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)

                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS engine returned ERROR for text: $text")
                    FirebaseCrashlytics.getInstance().log("TTS engine returned ERROR for text: $text")
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.LogNonFatalError(TAG, "An error occurred while speaking", e)
        } finally {
            audioManager.restoreVolume()
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
        audioManager.restoreVolume()
    }

    fun shutdown() {
        tts?.shutdown()
        isInitialized = false
    }
}
