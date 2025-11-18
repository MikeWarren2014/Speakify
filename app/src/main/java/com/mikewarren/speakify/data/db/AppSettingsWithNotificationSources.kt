package com.mikewarren.speakify.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class AppSettingsWithNotificationSources(
    @Embedded val appSettings: AppSettingsDbModel,
    @Relation(
        parentColumn = "as_id",
        entityColumn = "as_id"
    )
    val notificationSources: List<NotificationSourceModel>,
)