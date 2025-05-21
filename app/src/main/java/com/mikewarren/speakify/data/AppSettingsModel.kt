package com.mikewarren.speakify.data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsModel(
    val id: Long?,
    val packageName: String,
    val announcerVoice: String?, // Nullable if no voice is selected

    val notificationSources: List<String> = emptyList(),
) {
    companion object {
        fun FromDbModel(dbModel: AppSettingsWithNotificationSources): AppSettingsModel {
            return AppSettingsModel(
                id = dbModel.appSettings.id!!,
                packageName = dbModel.appSettings.packageName,
                announcerVoice = dbModel.appSettings.announcerVoice,
                notificationSources = dbModel.notificationSources.map { it.value }
            )
        }
    }
}
