package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mikewarren.speakify.data.AppSettingsWithNotificationSources

@Dao
interface AppSettingsDao {

    @Transaction
    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingsWithNotificationSources>

    @Transaction
    @Query("SELECT * FROM app_settings WHERE package_name = :packageName")
    suspend fun getByPackageName(packageName: String): AppSettingsWithNotificationSources?


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(appSettings: AppSettingsDbModel): Long

    /**
     * Updates an AppSettings entity.
     * Use REPLACE strategy to handle potential conflicts (e.g., if packageName is unique)
     */
    @Update
    suspend fun updateAppSettings(appSettings: AppSettingsDbModel)


    @Transaction
    @Query("DELETE FROM app_settings WHERE package_name = :packageName")
    suspend fun deleteAppSettingsByPackageName(packageName: String)


}