package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import androidx.room.OnConflictStrategy

@Dao
interface NotificationSourcesDao {
    @Query("SELECT * FROM notification_sources")
    suspend fun getAll(): List<NotificationSourceModel>
    @Query("SELECT * FROM notification_sources WHERE as_id = :appSettingsId")
    suspend fun getByAppSettingsId(appSettingsId: Long): List<NotificationSourceModel>

    @Query("DELETE FROM notification_sources WHERE as_id = :appSettingsId")
    suspend fun deleteByAppSettingsId(appSettingsId: Long)

    @Query("DELETE FROM notification_sources WHERE as_id = :appSettingsId AND ns_value NOT IN (:values)")
    suspend fun deleteAllWithoutValues(appSettingsId: Long, values : List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notificationSources: List<NotificationSourceModel>)

}