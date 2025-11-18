package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.AppSettingsWithNotificationSources
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsModel(
    val id: Long?,
    val packageName: String,
    val announcerVoice: String?, // Nullable if no voice is selected

    val notificationSources: List<String> = emptyList(),
) {
    constructor(packageName: String, announcerVoice: String?) : this(-1, packageName, announcerVoice)

    companion object {
        fun FromDbModel(dbModel: AppSettingsWithNotificationSources?): AppSettingsModel? {
            if (dbModel == null)
                return null

            return AppSettingsModel(
                id = dbModel.appSettings.id!!,
                packageName = dbModel.appSettings.packageName,
                announcerVoice = dbModel.appSettings.announcerVoice,
                notificationSources = dbModel.notificationSources.map { it.value }
            )
        }
    }
}
