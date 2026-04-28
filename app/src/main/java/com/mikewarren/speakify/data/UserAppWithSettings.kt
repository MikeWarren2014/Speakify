package com.mikewarren.speakify.data

import androidx.room.Embedded
import androidx.room.Relation
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.UserAppModel

data class UserAppWithSettings(
    @Embedded val userApp: UserAppModel,
    @Relation(
        parentColumn = "ua_id",
        entityColumn = "ua_id"
    )
    val appSettings: AppSettingsDbModel,
)
