package com.mikewarren.speakify

import android.content.Context
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.AppsRepositoryImpl
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.SettingsRepositoryImpl
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
            @ApplicationContext context: Context
        ): SettingsRepositoryImpl = SettingsRepositoryImpl(context)

        @Provides
        @Singleton
        fun provideAppsRepository(): AppsRepositoryImpl = AppsRepositoryImpl()
    }
}