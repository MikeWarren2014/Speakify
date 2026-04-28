package com.mikewarren.speakify.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class AppSettingsWithNotificationSources(
    @Embedded val appSettings: AppSettingsDbModel,
    @Relation(
        parentColumn = "ua_id",
        entityColumn = "ua_id"
    )
    val userApp: UserAppModel?,
    @Relation(
        parentColumn = "as_id",
        entityColumn = "as_id"
    )
    val notificationSources: List<NotificationSourceModel>,
)
