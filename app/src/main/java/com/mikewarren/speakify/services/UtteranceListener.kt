package com.mikewarren.speakify.services

import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
class UtteranceListener(private val utteranceId: String,
                        private val continuation: CancellableContinuation<Boolean>,
    private val TAG: String = "UtteranceListener"):
    UtteranceProgressListener() {
    override fun onStart(id: String?) {}

    override fun onDone(id: String?) {
        if (id == utteranceId && continuation.isActive) {
            continuation.resume(true)
        }
    }

    @Deprecated("deprecated in API level 21")
    override fun onError(id: String?) {
        if (id == utteranceId && continuation.isActive) {
            Log.e(TAG, "TTS error for utterance: $id")
            continuation.resume(false)
        }
    }

    override fun onError(id: String?, errorCode: Int) {
        if (id == utteranceId && continuation.isActive) {
            Log.e(TAG, "TTS error with code $errorCode for utterance: $id")
            continuation.resume(false)
        }
    }

    override fun onStop(id: String?, interrupted: Boolean) {
        if (id == utteranceId && continuation.isActive) {
            Log.d(TAG, "TTS utterance stopped: $id")
            continuation.resume(false)
        }
    }
}