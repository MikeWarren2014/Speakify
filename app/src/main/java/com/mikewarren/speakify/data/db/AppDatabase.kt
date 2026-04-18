package com.mikewarren.speakify.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserAppModel::class,
        AppSettingsDbModel::class,
        NotificationSourceModel::class,
        RecentMessengerContactModel::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ],
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAppsDao(): UserAppsDao
    abstract fun notificationSourcesDao(): NotificationSourcesDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun recentMessengerContactDao(): RecentMessengerContactDao
}
