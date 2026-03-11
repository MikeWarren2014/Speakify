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
     * This migration cleans up orphaned rows in app_settings before the foreign key constraint is applied,
     * and manually performs the schema changes (adding the Foreign Key and Index).
     *
     * Note: Manual migrations (Migration) take precedence over AutoMigrations. Since we provide this
     * manual migration, the AutoMigration(from = 1, to = 2) defined in AppDatabase is ignored.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Clean up orphaned app_settings rows that don't have a matching important_apps row.
            // This prevents the "Foreign key violation" error.
            db.execSQL("""
                DELETE FROM app_settings 
                WHERE package_name NOT IN (SELECT package_name FROM important_apps)
            """.trimIndent())

            // 2. Create a temporary table with the new schema (including the Foreign Key constraint).
            // This matches the schema expected in Version 2.
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `app_settings_new` (
                    `as_id` INTEGER PRIMARY KEY AUTOINCREMENT, 
                    `package_name` TEXT NOT NULL, 
                    `announcer_voice` TEXT, 
                    FOREIGN KEY(`package_name`) REFERENCES `important_apps`(`package_name`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())

            // 3. Copy data from the old table to the new one.
            db.execSQL("""
                INSERT INTO `app_settings_new` (`as_id`, `package_name`, `announcer_voice`)
                SELECT `as_id`, `package_name`, `announcer_voice` FROM `app_settings`
            """.trimIndent())

            // 4. Drop the old table.
            db.execSQL("DROP TABLE `app_settings`")

            // 5. Rename the new table to the original name.
            db.execSQL("ALTER TABLE `app_settings_new` RENAME TO `app_settings`")

            // 6. Create the index required by the new version.
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_app_settings_package_name` ON `app_settings` (`package_name`)")
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
