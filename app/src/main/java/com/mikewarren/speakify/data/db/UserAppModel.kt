package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mikewarren.speakify.data.Constants
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "important_apps")
data class UserAppModel(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "enabled") var enabled: Boolean = true,
    @ColumnInfo(name = "rate_limit") var rateLimit: Long = 2 * Constants.OneSecond, // milliseconds between notifications
)