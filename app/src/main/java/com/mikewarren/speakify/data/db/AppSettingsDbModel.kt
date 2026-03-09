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
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["package_name"], unique = true)],
)
data class AppSettingsDbModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "as_id") val id: Long?,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "announcer_voice") val announcerVoice: String?, // Nullable if no voice is selected
    @ColumnInfo(name = "additional_settings", defaultValue = "{}") val additionalSettings: Map<String, String> = emptyMap(),
) {
    constructor(): this(null,
        "",
        null,
        emptyMap())
}
