package com.mikewarren.speakify.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbProvider {
    private var _db: AppDatabase? = null

    const val DbName = "speakify-db"

    /**
     * Migration from version 1 to 2.
     * This migration cleans up orphaned rows in app_settings before the foreign key constraint is applied.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Clean up orphaned app_settings rows that don't have a matching important_apps row
            db.execSQL("""
                DELETE FROM app_settings 
                WHERE package_name NOT IN (SELECT package_name FROM important_apps)
            """.trimIndent())
            
            // Note: AutoMigration will handle the rest of the schema changes (adding constraints, etc.)
            // as long as the data is clean.
        }
    }

    fun GetDb(context: Context) : AppDatabase {
        if (this._db == null) {
            this._db = Room.databaseBuilder(
                context,
                AppDatabase::class.java, DbName
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        return this._db!!
    }
}