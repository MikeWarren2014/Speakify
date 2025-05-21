package com.mikewarren.speakify.data

import androidx.room.Embedded
import androidx.room.Relation
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.UserAppModel

data class UserAppWithSettings(
    @Embedded val userApp: UserAppModel,
    @Relation(
        parentColumn = "package_name",
        entityColumn = "package_name"
    )
    val appSettings: AppSettingsDbModel,
)
