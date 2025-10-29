package com.mikewarren.speakify.services

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.utils.TTSUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingAnnouncements = mutableListOf<Pair<String, String?>>()

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
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
            })
            Log.d("TTSManager", "TTS engine successfully initialized.")
            // If there were any pending announcements, speak them now.
            pendingAnnouncements.forEach { (text, voiceName) ->
                // We can't use the suspend function here as we are not in a coroutine
                internalSpeak(text, voiceName)
            }
            pendingAnnouncements.clear()

            return
        }
        isInitialized = false
        Log.e("TTSManager", "TTS engine failed to initialize with status: $status")
    }

    suspend fun speak(text: String, voiceName: String? = null) {
        if (!isInitialized) {
            // Engine isn't ready yet, save the announcement to be spoken once it is.
            pendingAnnouncements.add(text to voiceName)
            Log.d("TTSManager", "TTS not ready. Queuing announcement.")
            return
        }

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
            tts?.setOnUtteranceProgressListener(listener)
            TTSUtils.SetTTSVoice(tts, voiceName)
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }
    }

    private fun internalSpeak(text: String, voiceName: String? = null) {
        TTSUtils.SetTTSVoice(tts, voiceName)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
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
