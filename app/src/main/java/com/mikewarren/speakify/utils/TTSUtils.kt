package com.mikewarren.speakify.utils

import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Locale

object TTSUtils {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun GetRecommendedDefaultVoiceNames(ttsEngine: TextToSpeech): List<String> {
        val availableVoices = ttsEngine.voices?.toList() ?: emptyList()
        val usEnglishVoices = availableVoices.filter {
            it.locale == Locale.US && it.name.contains("en-us", ignoreCase = true)
        }

        if (usEnglishVoices.isEmpty()) {
            Log.w("SettingsView", "No en-US voices found, falling back to system default.")
            return emptyList() // Or return a list containing the system default voice
        }

        // Prioritize voices based on name (this is highly engine-dependent and might need adjustments)
        val preferredNames = listOf("female", "male", "en-us") // Add names of voices you think sound good
        val preferredVoices = usEnglishVoices.filter { voice ->
            preferredNames.any { preferredName ->
                voice.name.contains(preferredName, ignoreCase = true)
            }
        }

        // If no preferred voices found, fallback to first two en-US voices
        var voices = preferredVoices
        if (!preferredVoices.isNullOrEmpty())
            voices = usEnglishVoices

        return voices.map { voice: Voice -> voice.name}
    }
}