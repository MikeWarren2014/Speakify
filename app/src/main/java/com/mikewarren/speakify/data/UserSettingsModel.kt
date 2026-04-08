package com.mikewarren.speakify.data

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
    val maximizeVolumeOnScreenOff: Boolean = false,
    val minVolume: Int = 0,

    val isCrashlyticsEnabled: Boolean = false,
    val originalVolume: Int = -1,

    val scheduling: SchedulingModel = SchedulingModel(),

    val trialModel: TrialModel = TrialModel(),
) {
    constructor() : this(
        useDarkTheme = true,
        selectedTTSVoice = Constants.DefaultTTSVoice,
        maximizeVolumeOnScreenOff = false,
    )
}
