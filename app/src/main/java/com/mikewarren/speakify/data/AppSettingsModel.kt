package com.mikewarren.speakify.data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsModel(
    val packageName: String,
    val announcerVoice: String?, // Nullable if no voice is selected
    val notificationSources: List<String> = emptyList()
)
