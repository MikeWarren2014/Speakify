package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recent_email_contacts")
data class RecentEmailContactModel(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "last_seen") val lastSeen: Long = System.currentTimeMillis()
)
