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

    /**
     * Migration from version 5 to 6.
     * This migration handles the change of primary key in 'important_apps' from 'package_name' to 'ua_id',
     * and updates 'app_settings' to use 'ua_id' as a foreign key instead of 'package_name'.
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create all new tables first to avoid breaking Foreign Key constraints
            // We use DROP TABLE IF EXISTS to ensure any previous failed attempts are cleared.
            db.execSQL("DROP TABLE IF EXISTS `important_apps_new`")
            db.execSQL("""
                CREATE TABLE `important_apps_new` (
                    `ua_id` INTEGER PRIMARY KEY AUTOINCREMENT, 
                    `package_name` TEXT NOT NULL, 
                    `app_name` TEXT NOT NULL, 
                    `enabled` INTEGER NOT NULL, 
                    `rate_limit` INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("DROP TABLE IF EXISTS `app_settings_new`")
            db.execSQL("""
                CREATE TABLE `app_settings_new` (
                    `as_id` INTEGER PRIMARY KEY AUTOINCREMENT, 
                    `ua_id` INTEGER, 
                    `announcer_voice` TEXT, 
                    `additional_settings` TEXT NOT NULL DEFAULT '{}', 
                    FOREIGN KEY(`ua_id`) REFERENCES `important_apps`(`ua_id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())

            db.execSQL("DROP TABLE IF EXISTS `notification_sources_new`")
            db.execSQL("""
                CREATE TABLE `notification_sources_new` (
                    `ns_id` INTEGER, 
                    `as_id` INTEGER, 
                    `ns_value` TEXT NOT NULL, 
                    `ns_name` TEXT, 
                    PRIMARY KEY(`ns_id`), 
                    FOREIGN KEY(`as_id`) REFERENCES `app_settings`(`as_id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())

            // 2. Transfer data into the new tables
            db.execSQL("""
                INSERT INTO `important_apps_new` (`package_name`, `app_name`, `enabled`, `rate_limit`)
                SELECT `package_name`, `app_name`, `enabled`, `rate_limit` FROM `important_apps`
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `app_settings_new` (`as_id`, `ua_id`, `announcer_voice`, `additional_settings`)
                SELECT s.`as_id`, i.`ua_id`, s.`announcer_voice`, s.`additional_settings`
                FROM `app_settings` s
                JOIN `important_apps_new` i ON s.`package_name` = i.`package_name`
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `notification_sources_new` (`ns_id`, `as_id`, `ns_value`, `ns_name`)
                SELECT `ns_id`, `as_id`, `ns_value`, `ns_name` FROM `notification_sources`
            """.trimIndent())

            // 3. Drop old tables in reverse order of dependency
            db.execSQL("DROP TABLE `notification_sources`")
            db.execSQL("DROP TABLE `app_settings`")
            db.execSQL("DROP TABLE `important_apps`")

            // 4. Rename new tables to their final names
            db.execSQL("ALTER TABLE `important_apps_new` RENAME TO `important_apps`")
            db.execSQL("ALTER TABLE `app_settings_new` RENAME TO `app_settings`")
            db.execSQL("ALTER TABLE `notification_sources_new` RENAME TO `notification_sources`")

            // 5. Create required indices
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_important_apps_package_name` ON `important_apps` (`package_name`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_app_settings_ua_id` ON `app_settings` (`ua_id`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notification_sources_as_id_ns_value` ON `notification_sources` (`as_id`, `ns_value`)")
        }
    }

    /**
     * Migration from version 6 to 7.
     * This migration acts as a cleanup to ensure that even if version 6 was left in a "dirty" state 
     * (e.g., by an AutoMigration that didn't remove the 'package_name' column), version 7 will have 
     * the correct clean schema.
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Re-create app_settings and notification_sources to ensure a clean schema for version 7.
            // This acts as a cleanup to fix any "dirty" state from previous migrations (like incorrect FK references).

            // 1. Re-create app_settings
            db.execSQL("DROP TABLE IF EXISTS `app_settings_new`")
            db.execSQL("""
                CREATE TABLE `app_settings_new` (
                    `as_id` INTEGER PRIMARY KEY AUTOINCREMENT, 
                    `ua_id` INTEGER, 
                    `announcer_voice` TEXT, 
                    `additional_settings` TEXT NOT NULL DEFAULT '{}', 
                    FOREIGN KEY(`ua_id`) REFERENCES `important_apps`(`ua_id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())

            // Check if app_settings has the old package_name column to avoid 'no such column' error if we did a partial select
            val cursor = db.query("PRAGMA table_info(app_settings)")
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            cursor.close()

            val selectColumns = listOf("as_id", "ua_id", "announcer_voice", "additional_settings")
                .filter { it in columns }
                .joinToString(", ")

            db.execSQL("""
                INSERT INTO `app_settings_new` ($selectColumns)
                SELECT $selectColumns FROM `app_settings`
            """.trimIndent())

            // 2. Re-create notification_sources
            db.execSQL("DROP TABLE IF EXISTS `notification_sources_new`")
            db.execSQL("""
                CREATE TABLE `notification_sources_new` (
                    `ns_id` INTEGER, 
                    `as_id` INTEGER, 
                    `ns_value` TEXT NOT NULL, 
                    `ns_name` TEXT, 
                    PRIMARY KEY(`ns_id`), 
                    FOREIGN KEY(`as_id`) REFERENCES `app_settings`(`as_id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("INSERT INTO `notification_sources_new` SELECT * FROM `notification_sources`")

            // 3. Swap tables
            db.execSQL("DROP TABLE `notification_sources`")
            db.execSQL("DROP TABLE `app_settings`")

            db.execSQL("ALTER TABLE `app_settings_new` RENAME TO `app_settings`")
            db.execSQL("ALTER TABLE `notification_sources_new` RENAME TO `notification_sources`")

            // 4. Re-create indices
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_app_settings_ua_id` ON `app_settings` (`ua_id`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notification_sources_as_id_ns_value` ON `notification_sources` (`as_id`, `ns_value`)")
        }
    }

    fun GetDb(context: Context) : AppDatabase {
        if (this._db == null) {
            this._db = Room.databaseBuilder(
                context,
                AppDatabase::class.java, DbName
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_5_6, MIGRATION_6_7)
                .build()
        }

        return this._db!!
    }
}
