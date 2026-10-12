package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.models.AppCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppCategoryRepository {
    val packageCategories: StateFlow<Map<String, AppCategory>>

    suspend fun getCategoryForPackage(packageName: String): AppCategory?
    suspend fun getCategoriesForPackages(packageNames: List<String>): Map<String, AppCategory>
    suspend fun initializeCategories()
    suspend fun addCategory(packageName: String, category: AppCategory)
}
