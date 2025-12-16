package com.mikewarren.speakify.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class UserSettingsModel(
    val useDarkTheme: Boolean,
    val selectedTTSVoice: String,
    val appSettings: Map<String, AppSettingsModel>,
    val maximizeVolumeOnScreenOff: Boolean = false,
    val minVolume: Int = 0,

    val hasRequestedPhonePermissions: Boolean = false,
    val isCrashlyticsEnabled: Boolean = false,
    val originalVolume: Int = -1,
)
