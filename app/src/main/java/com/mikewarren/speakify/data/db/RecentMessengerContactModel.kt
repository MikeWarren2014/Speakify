package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recent_messenger_contacts")
data class RecentMessengerContactModel(
    @PrimaryKey @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "last_seen") val lastSeen: Long = System.currentTimeMillis()
)
