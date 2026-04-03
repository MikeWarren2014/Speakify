package com.mikewarren.speakify.utils

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.models.VoiceInfoModel
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import java.util.Locale

object TTSUtils: ITaggable {

    /**
     * Retrieves a list of recommended voices based on the current system locale.
     */
    fun GetRecommendedDefaultVoiceModels(ttsEngine: TextToSpeech): List<VoiceInfoModel> {
        val availableVoices = ttsEngine.voices?.toList() ?: emptyList()
        val systemLocale = Locale.getDefault()

        // 1. Try to find voices matching the user's current system locale
        var recommendedVoices = availableVoices.filter {
            it.locale.language == systemLocale.language
        }

        // 2. If no local voices, fallback to US English as a secondary recommendation
        if (recommendedVoices.isEmpty() && systemLocale != Locale.US) {
            recommendedVoices = availableVoices.filter {
                it.locale.language == Locale.US.language
            }
        }

        // Prioritize voices that are "high quality" (not network dependent if possible)
        // and try to provide a mix of genders if the engine provides that info in the name.
        val prioritized = recommendedVoices.sortedByDescending { it.quality }

        return prioritized.mapNotNull { voice: Voice -> ToVoiceInfoModel(voice) }
    }

    fun ToVoiceInfoModel(voice: Voice): VoiceInfoModel? {
        return voice.locale?.let { locale ->
            VoiceInfoModel(
                name = voice.name,
                displayName = "${locale.displayName} (${voice.name})", 
                language = locale.language,
                country = locale.displayCountry
            )
        }
    }

    /**
     * Sets the TTS voice. If voiceName is null or empty, it attempts to use the 
     * best voice for the current system locale.
     */
    fun SetTTSVoice(tts: TextToSpeech?, voiceName: String? = Constants.DefaultTTSVoice) {
        val voices = tts?.voices
        if (voices.isNullOrEmpty()) {
            LogUtils.LogWarning(TAG, "TTS engine has no voices available.")
            return
        }

        val systemLocale = Locale.getDefault()

        // 1. Try to find the exact voice requested
        var selectedVoice: Voice? = if (!voiceName.isNullOrBlank()) {
            voices.find { it.name == voiceName }
        } else {
            null
        }

        // 2. If no specific voice requested (or not found), find best for system locale
        if (selectedVoice == null) {
            selectedVoice = voices.find { it.locale == systemLocale } 
                ?: voices.find { it.locale.language == systemLocale.language }
                ?: voices.firstOrNull()
        }

        selectedVoice?.let {
            LogUtils.LogBreadcrumb(TAG, "Setting TTS voice to: ${it.name} (${it.locale})")
            tts.voice = it
        }
    }
}
