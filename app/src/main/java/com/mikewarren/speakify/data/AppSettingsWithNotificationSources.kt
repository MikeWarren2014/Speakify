package com.mikewarren.speakify.data

import androidx.room.Embedded
import androidx.room.Relation
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.NotificationSourceModel

data class AppSettingsWithNotificationSources(
    @Embedded val appSettings: AppSettingsDbModel,
    @Relation(
        parentColumn = "as_id",
        entityColumn = "as_id"
    )
    val notificationSources: List<NotificationSourceModel>,
)
