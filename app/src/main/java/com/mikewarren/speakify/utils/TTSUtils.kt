package com.mikewarren.speakify.utils

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.models.VoiceInfoModel
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import java.util.Locale

object TTSUtils: ITaggable {

    fun GetRecommendedDefaultVoiceModels(ttsEngine: TextToSpeech): List<VoiceInfoModel> {
        val availableVoices = ttsEngine.voices?.toList() ?: emptyList()
        val usEnglishVoices = availableVoices.filter {
            it.locale == Locale.US && it.name.contains("en-us", ignoreCase = true)
        }

        if (usEnglishVoices.isEmpty()) {
            LogUtils.LogWarning(TAG, "No en-US voices found, falling back to system default.")
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
        if (preferredVoices.isNotEmpty())
            voices = usEnglishVoices

        return voices.mapNotNull { voice: Voice -> ToVoiceInfoModel(voice) }
    }

    fun ToVoiceInfoModel(voice: Voice): VoiceInfoModel? {
        return voice.locale?.let { locale ->
            VoiceInfoModel(
                name = voice.name,
                displayName = locale.displayLanguage, // "English"
                language = locale.language,           // "en"
                country = locale.displayCountry       // "United States"
            )
        }
    }

    fun SetTTSVoice(tts: TextToSpeech?, voiceName: String? = Constants.DefaultTTSVoice) {
        val voices = tts?.voices
        if (voices.isNullOrEmpty()) {
            LogUtils.LogWarning(TAG, "TTS engine has no voices available, using system default.")
            return
        }

        // Use provided voiceName or system default if null
        val voice = voiceName?.let { name ->
            voices.find { it.name == name }
        } ?: voices.find { it.locale == Locale.getDefault() } ?: voices.firstOrNull()

        voice?.let {
            tts.voice = it
        }
    }
}