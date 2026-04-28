package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "app_settings",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = UserAppModel::class,
            parentColumns = ["ua_id"],
            childColumns = ["ua_id"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["ua_id"], unique = true)],
)
data class AppSettingsDbModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "as_id") val id: Long?,
    @ColumnInfo(name = "ua_id") val userAppId: Long?,
    @ColumnInfo(name = "announcer_voice") val announcerVoice: String?, // Nullable if no voice is selected
    @ColumnInfo(name = "additional_settings", defaultValue = "{}") val additionalSettings: Map<String, String> = emptyMap(),
) {
    constructor(): this(null,
        null,
        null,
        emptyMap())
}
