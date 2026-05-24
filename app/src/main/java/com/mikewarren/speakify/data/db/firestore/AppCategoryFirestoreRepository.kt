package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.AppCategoryModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCategoryFirestoreRepository @Inject constructor() : BaseFirestoreRepository() {

    private val categoriesCollection = firestore.collection("appCategories")

    suspend fun fetchAllCategories(): List<AppCategoryModel> {
        return try {
            safeFirestoreCall {
                categoriesCollection.get().await().documents.mapNotNull { doc ->
                    val packageName = doc.getString("packageName") ?: return@mapNotNull null
                    val categoryName = doc.getString("category") ?: return@mapNotNull null
                    val category = try {
                        AppCategory.valueOf(categoryName)
                    } catch (e: Exception) {
                        // Handle "Business/Productivity" case if it's stored that way
                        AppCategory.entries.find { it.categoryName == categoryName } ?: return@mapNotNull null
                    }
                    AppCategoryModel(packageName = packageName, appCategory = category)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadCategory(categoryModel: AppCategoryModel) {
        try {
            safeFirestoreCall {
                val data = hashMapOf(
                    "packageName" to categoryModel.packageName,
                    "category" to categoryModel.appCategory.name
                )
                // Use packageName as document ID to avoid duplicates
                categoriesCollection.document(categoryModel.packageName).set(data).await()
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
