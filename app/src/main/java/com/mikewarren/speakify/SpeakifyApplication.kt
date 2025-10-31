package com.mikewarren.speakify

import android.app.Application
import com.clerk.api.Clerk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpeakifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(
            this,
            publishableKey = "pk_test_Z2xvcmlvdXMtbGxhbWEtNzQuY2xlcmsuYWNjb3VudHMuZGV2JA"

        )
    }
}