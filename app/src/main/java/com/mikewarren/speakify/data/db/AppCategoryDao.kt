package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.AppCategoryModel
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Query("SELECT * FROM app_categories")
    fun getAll(): Flow<List<AppCategoryModel>>

    @Query("SELECT appCategory FROM app_categories WHERE packageName = :packageName LIMIT 1")
    suspend fun getCategoryForPackage(packageName: String): AppCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<AppCategoryModel>)

    @Query("DELETE FROM app_categories")
    suspend fun deleteAll()
}
