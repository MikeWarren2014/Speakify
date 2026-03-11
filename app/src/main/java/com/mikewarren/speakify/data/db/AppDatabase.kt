package com.mikewarren.speakify.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserAppModel::class, AppSettingsDbModel::class, NotificationSourceModel::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAppsDao(): UserAppsDao
    abstract fun notificationSourcesDao(): NotificationSourcesDao
    abstract fun appSettingsDao(): AppSettingsDao

}