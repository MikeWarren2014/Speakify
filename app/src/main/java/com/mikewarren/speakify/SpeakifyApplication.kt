package com.mikewarren.speakify

import android.app.Application
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpeakifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("SpeakifyApp", "SpeakifyApplication.onCreate started")
        Clerk.initialize(
            this,
            publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY,
            options = ClerkConfigurationOptions(
                enableDebugMode = false
            ),
        )
        Log.d("SpeakifyApp", "Clerk initialized")
    }
}
