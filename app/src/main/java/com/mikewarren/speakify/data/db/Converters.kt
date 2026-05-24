package com.mikewarren.speakify.data.db

import androidx.room.TypeConverter
import com.mikewarren.speakify.data.models.AppCategory
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromAppCategory(value: AppCategory): String {
        return value.name
    }

    @TypeConverter
    fun toAppCategory(value: String): AppCategory {
        return AppCategory.valueOf(value)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            Json.decodeFromString<Map<String, String>>(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
