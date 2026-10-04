package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentEmailContactDao {
    @Query("SELECT * FROM recent_email_contacts ORDER BY last_seen DESC LIMIT 50")
    fun getRecentContacts(): Flow<List<RecentEmailContactModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<RecentEmailContactModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: RecentEmailContactModel)

    @Query("DELETE FROM recent_email_contacts")
    suspend fun clearAll()

    @Query("""
        DELETE FROM recent_email_contacts 
        WHERE name NOT IN (
            SELECT name FROM recent_email_contacts 
            ORDER BY last_seen DESC 
            LIMIT 100
        )
    """)
    suspend fun pruneOldContacts()
}
