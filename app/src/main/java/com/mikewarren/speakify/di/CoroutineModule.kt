package com.mikewarren.speakify.di

import com.mikewarren.speakify.utils.DefaultDispatcherProvider
import com.mikewarren.speakify.utils.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(dispatcherProvider: DispatcherProvider): CoroutineScope {
        // SupervisorJob makes sure that if one coroutine fails, the whole scope is not cancelled.
        return CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    }
}
