package com.mikewarren.speakify.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "app_categories")
@Serializable
data class AppCategoryModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appCategory: AppCategory
)

@Serializable
enum class AppCategory(val categoryName: String) {
    Communication("Communication"),
    BusinessProductivity("Business/Productivity"),
    Shopping("Shopping"),
    Other("Other"),
}
