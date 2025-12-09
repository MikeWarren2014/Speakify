package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.services.TTSManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseTTSAutoCompletableViewModel(
    protected open val settingsRepository: SettingsRepository) : BaseAutoCompletableViewModel() {
    // Text-to-speech settings
    @Inject
    lateinit var ttsManager: TTSManager

    override fun getLabel(): String {
        return "TTS Voice"
    }

    override fun getAllChoices(): List<String> {
        return ttsManager.getAllVoiceNames()
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

    protected fun setTTSVoice(voiceName: String? = Constants.DefaultTTSVoice) {
        ttsManager.setVoice(voiceName)
    }

    abstract fun onSelectedVoice(voiceName: String)

}