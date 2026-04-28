package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mikewarren.speakify.data.Constants
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "important_apps",
    indices = [androidx.room.Index(value = ["package_name"], unique = true)]
)
data class UserAppModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "ua_id") val id: Long? = null,
    @ColumnInfo(name = "package_name") var packageName: String,
    @ColumnInfo(name = "app_name") var appName: String,
    @ColumnInfo(name = "enabled") var enabled: Boolean = true,
    @ColumnInfo(name = "rate_limit") var rateLimit: Long = 2 * Constants.OneSecond, // milliseconds between notifications
) {
    constructor(): this(null,
        "",
        "",
        true,
        2 * Constants.OneSecond)
}
