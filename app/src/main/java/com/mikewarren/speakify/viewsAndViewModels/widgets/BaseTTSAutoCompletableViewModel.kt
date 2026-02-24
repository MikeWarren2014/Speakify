package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.models.VoiceInfoModel
import com.mikewarren.speakify.services.TTSManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseTTSAutoCompletableViewModel(
    protected open val settingsRepository: SettingsRepository,
    val ttsManager: TTSManager,
) : BaseModelAutoCompletableViewModel<VoiceInfoModel>() {

    protected val choicesMap: Map<String, VoiceInfoModel> by lazy {
        getAllChoices().associateBy { toSourceString(it) }
    }

    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_tts)
    }

    override fun toViewString(sourceModel: VoiceInfoModel): String {
        return "${sourceModel.displayName} (${sourceModel.country})"
    }

    override fun toSourceString(sourceModel: VoiceInfoModel): String {
        return sourceModel.name
    }

    override fun getAllChoices(): List<VoiceInfoModel> {
        return ttsManager.getVoiceInfoList()
    }

    protected fun observeVoicePreference() {
        viewModelScope.launch {
            getTTSFlow().collectLatest { voiceName ->
                val voiceInfo = voiceName?.let { choicesMap[it] }
                selection = voiceInfo
                setTTSVoice(voiceName)

                // Use display name if model exists, otherwise fallback to name or default
                searchText = voiceInfo?.let { toViewString(it) } ?: voiceName ?: Constants.DefaultTTSVoice
            }
        }
    }

    protected abstract fun getTTSFlow(): Flow<String?>

    protected fun setTTSVoice(voiceName: String? = Constants.DefaultTTSVoice) {
        ttsManager.setVoice(voiceName)
    }

    abstract fun onSelectedVoice(voiceName: String)
}
