package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserAppsDao {
    @Query("SELECT * FROM important_apps")
    suspend fun getAll(): List<UserAppModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(importantApp: UserAppModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg importantApps: UserAppModel)

    @Delete
    suspend fun deleteAll(vararg importantApps: UserAppModel)
}