package com.mikewarren.speakify.data.db

import com.mikewarren.speakify.data.models.AppCategoryModel
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserAppModel::class,
        AppSettingsDbModel::class,
        NotificationSourceModel::class,
        RecentMessengerContactModel::class,
        RecentEmailContactModel::class,
        AppCategoryModel::class,
        NotificationAuditLogModel::class
    ],
    version = 10,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10)
    ],
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userAppsDao(): UserAppsDao
    abstract fun notificationSourcesDao(): NotificationSourcesDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun recentMessengerContactDao(): RecentMessengerContactDao
    abstract fun recentEmailContactDao(): RecentEmailContactDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun notificationAuditLogDao(): NotificationAuditLogDao
}
