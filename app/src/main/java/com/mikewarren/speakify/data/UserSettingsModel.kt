package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.models.OnboardingModel
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class UserSettingsModel(
    val useDarkTheme: Boolean,
    val selectedTTSVoice: String,
    val maximizeVolumeOnScreenOff: Boolean = Constants.DefaultBooleanSetting,
    val stopSpeechOnScreenOff: Boolean = Constants.DefaultBooleanSetting,
    val minVolume: Int = 0,

    val isCrashlyticsEnabled: Boolean = Constants.DefaultBooleanSetting,
    val originalVolume: Int = -1,

    val scheduling: SchedulingModel = SchedulingModel(),

    val trialModel: TrialModel = TrialModel(),

    val onboardingModel: OnboardingModel = OnboardingModel(),
) {
    constructor() : this(
        useDarkTheme = true,
        selectedTTSVoice = Constants.DefaultTTSVoice,
        maximizeVolumeOnScreenOff = Constants.DefaultBooleanSetting,
    )
}
