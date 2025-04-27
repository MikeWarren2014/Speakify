package com.mikewarren.speakify.viewsAndViewModels.widgets

import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

abstract class BaseTTSAutoCompletableViewModel(
    protected open val settingsRepository: SettingsRepository) : BaseAutoCompletableViewModel() {
    // Text-to-speech settings
    var tts: TextToSpeech? = null

    override fun getLabel(): String {
        return "TTS Voice"
    }

    override fun getAllChoices(): List<String> {
        if ((tts == null) || (tts?.voices == null))
            return emptyList()

        return tts!!.voices
            .toList()
            .map { voice: Voice -> voice.name }
    }

    fun initializeTTS() {
        tts = TextToSpeech(settingsRepository.getContext(), { status ->
            if (status == TextToSpeech.SUCCESS) {
                setTTSVoice() // Call setTTSVoice when TTS is ready
                observeVoicePreference()
            } else {
                Log.e(this.javaClass.name, "TTS initialization failed with status: $status")
            }
        })
    }

    protected fun observeVoicePreference() {
        viewModelScope.launch {
            getTTSFlow().collectLatest { voiceName ->
                selection = voiceName // Update the state here
                setTTSVoice(voiceName)
                searchText = voiceName ?: ""
            }
        }
    }

    protected abstract fun getTTSFlow(): Flow<String?>

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    protected fun setTTSVoice(voiceName: String? = null) {
        val voices = tts?.voices
        if (voices.isNullOrEmpty()) {
            throw IllegalStateException("somehow, our list of voices to choose from is either null or empty")
        }

        // Use provided voiceName or system default if null
        val voice = voiceName?.let { name ->
            voices.find { it.name == name }
        } ?: voices.find { it.locale == Locale.getDefault() } ?: voices.firstOrNull()

        voice?.let {
            tts?.voice = it
        }
    }

    abstract fun onSelectedVoice(voiceName: String)

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}