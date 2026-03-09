package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.AppSettingsWithNotificationSources
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsModel(
    val id: Long?,
    val packageName: String,
    val announcerVoice: String?, // Nullable if no voice is selected

    val notificationSources: List<String> = emptyList(),
    val additionalSettings: Map<String, String> = emptyMap(),
) {
    constructor(packageName: String, announcerVoice: String?) : this(-1, packageName, announcerVoice)

    /**
     * Helper to get a boolean setting from the additionalSettings map.
     */
    fun getBooleanSetting(key: String, defaultValue: Boolean = false): Boolean {
        return additionalSettings[key]?.toBoolean() ?: defaultValue
    }

    /**
     * Helper to create a copy of the model with an updated additional setting.
     */
    fun withSetting(key: String, value: String): AppSettingsModel {
        return copy(additionalSettings = additionalSettings + (key to value))
    }

    fun withSetting(key: String, value: Boolean): AppSettingsModel {
        return withSetting(key, value.toString())
    }

    companion object {
        fun FromDbModel(dbModel: AppSettingsWithNotificationSources?): AppSettingsModel? {
            if (dbModel == null)
                return null

            return AppSettingsModel(
                id = dbModel.appSettings.id!!,
                packageName = dbModel.appSettings.packageName,
                announcerVoice = dbModel.appSettings.announcerVoice,
                notificationSources = dbModel.notificationSources.map { it.value },
                additionalSettings = dbModel.appSettings.additionalSettings
            )
        }
    }
}
