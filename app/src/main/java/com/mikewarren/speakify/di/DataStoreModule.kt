package com.mikewarren.speakify.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.mikewarren.speakify.data.UserSettingsModel
import com.mikewarren.speakify.data.UserSettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserSettingsDataStore(
        @ApplicationContext context: Context,
        serializer: UserSettingsSerializer,
    ): DataStore<UserSettingsModel> {
        return DataStoreFactory.create(
            serializer = serializer,
            produceFile = { context.dataStoreFile("user_settings.json") }
        )
    }
}