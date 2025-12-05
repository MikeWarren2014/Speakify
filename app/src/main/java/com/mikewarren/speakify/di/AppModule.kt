package com.mikewarren.speakify.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.AppsRepositoryImpl
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.SettingsRepositoryImpl
import com.mikewarren.speakify.data.UserSettingsModel
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton // If you want a single instance
    abstract fun bindAppsRepository(
        appsRepositoryImpl: AppsRepositoryImpl
        // If the repository has dependencies, list them as parameters here
        // e.g., database: YourDatabase
    ): AppsRepository

    companion object {
        @Provides
        @Singleton
        fun provideSettingsRepositoryImpl(
            @ApplicationContext context: Context,
            userSettingsDataStore: DataStore<UserSettingsModel>,
        ): SettingsRepositoryImpl = SettingsRepositoryImpl(context, userSettingsDataStore)

        @Provides
        @Singleton
        fun provideAppsRepository(
            @ApplicationContext context: Context,
            
        ): AppsRepositoryImpl = AppsRepositoryImpl(context)

        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase = DbProvider.GetDb(context.applicationContext)

        @Provides
        @Singleton
        fun provideUserAppsDao(database: AppDatabase): UserAppsDao {
            return database.userAppsDao()
        }

        @Provides
        @Singleton
        fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao {
            return database.appSettingsDao()
        }

        @Provides
        @Singleton
        fun provideNotificationSourcesDao(database: AppDatabase): NotificationSourcesDao {
            return database.notificationSourcesDao()
        }

    }
}