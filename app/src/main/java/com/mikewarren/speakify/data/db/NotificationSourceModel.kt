package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "notification_sources",
    foreignKeys = [
        ForeignKey(
            entity = AppSettingsDbModel::class,
            parentColumns = ["as_id"], // Assuming 'as_id' is the PK in AppSettingsDbModel
            childColumns = ["as_id"],
            onDelete = ForeignKey.CASCADE // Or your desired action
        )
    ],
    indices = [androidx.room.Index(value = ["as_id", "ns_value"], unique = true)],
)
data class NotificationSourceModel(
    @PrimaryKey @ColumnInfo(name = "ns_id") val id: Long?,
    @ColumnInfo(name = "as_id") val appSettingsId: Long?,
    @ColumnInfo(name = "ns_value") val value: String,
)
