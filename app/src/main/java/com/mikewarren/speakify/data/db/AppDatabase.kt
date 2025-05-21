package com.mikewarren.speakify.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mikewarren.speakify.data.db.AppSettingsDbModel

@Database(
    entities = [UserAppModel::class, AppSettingsDbModel::class, NotificationSourceModel::class],
    version = 1,
    exportSchema = true,

)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAppsDao(): UserAppsDao
    abstract fun notificationSourcesDao(): NotificationSourcesDao
    abstract fun appSettingsDao(): AppSettingsDao

}