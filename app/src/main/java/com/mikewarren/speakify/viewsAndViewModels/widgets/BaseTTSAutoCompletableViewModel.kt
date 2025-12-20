package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.services.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseTTSAutoCompletableViewModel(
    protected open val settingsRepository: SettingsRepository,
    val ttsManager: TTSManager,
    ) : BaseAutoCompletableViewModel() {

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
                searchText = voiceName ?: Constants.DefaultTTSVoice
            }
        }
    }

    protected abstract fun getTTSFlow(): Flow<String?>

    protected fun setTTSVoice(voiceName: String? = Constants.DefaultTTSVoice) {
        ttsManager.setVoice(voiceName)
    }

    abstract fun onSelectedVoice(voiceName: String)

}