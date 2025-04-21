package com.mikewarren.speakify.data

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsModel(
    val useDarkTheme: Boolean,
    val selectedTTSVoice: String,
    val appSettings: Map<String, AppSettingsModel>,
)
