package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentMessengerContactDao {
    @Query("SELECT * FROM recent_messenger_contacts ORDER BY last_seen DESC LIMIT 50")
    fun getRecentContacts(): Flow<List<RecentMessengerContactModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<RecentMessengerContactModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: RecentMessengerContactModel)

    @Query("DELETE FROM recent_messenger_contacts")
    suspend fun clearAll()
}
