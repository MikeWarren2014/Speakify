package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAppsDao {
    @Query("SELECT * FROM important_apps")
    suspend fun getAll(): List<UserAppModel>

    @Query("SELECT * FROM important_apps")
    fun getAllFlow(): Flow<List<UserAppModel>>

    @Query("SELECT * FROM important_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): UserAppModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(importantApp: UserAppModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(importantApp: UserAppModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg importantApps: UserAppModel)

    @Delete
    suspend fun deleteAll(vararg importantApps: UserAppModel)

    @Update
    suspend fun update(importantApp: UserAppModel)
}