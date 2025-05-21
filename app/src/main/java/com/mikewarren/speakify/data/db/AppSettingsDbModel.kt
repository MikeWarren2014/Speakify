package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "app_settings",
    foreignKeys = [
          ForeignKey(
              entity = AppSettingsDbModel::class,
              parentColumns = ["as_id"],
              childColumns = ["as_id"],
              onDelete = ForeignKey.CASCADE,
          ),
    ],
    indices = [Index(value = ["package_name"], unique = true)],
    )
data class AppSettingsDbModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "as_id") val id: Long?,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "announcer_voice") val announcerVoice: String?, // Nullable if no voice is selected
)
