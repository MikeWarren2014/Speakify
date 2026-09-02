package com.mikewarren.speakify

import android.app.Application
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpeakifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("SpeakifyApp", "SpeakifyApplication.onCreate started")

        if (BuildConfig.DEBUG) {
            connectToEmulators()
        }

        Clerk.initialize(
            this,
            publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY,
            options = ClerkConfigurationOptions(
                enableDebugMode = false
            ),
        )
        Log.d("SpeakifyApp", "Clerk initialized")
    }

    private fun connectToEmulators() {
        val host = "10.0.2.2" // IP for Android Emulator to access localhost
        
        // Only attempt to connect to emulators if we are likely running in an emulator.
        // On a physical device, 10.0.2.2 is unreachable.
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic")
                || android.os.Build.FINGERPRINT.contains("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")

        if (!isEmulator) {
            Log.i("SpeakifyApp", "Physical device detected, skipping Firebase Emulators.")
            return
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()

            firestore.useEmulator(host, 8080)
            auth.useEmulator(host, 9099)

            // Disable persistence for local testing to ensure clean state
            firestore.firestoreSettings = firestoreSettings {
                isPersistenceEnabled = false
            }

            Log.d("SpeakifyApp", "Firebase Emulators connected at $host")
        } catch (e: Exception) {
            Log.e("SpeakifyApp", "Could not connect to Firebase Emulators", e)
        }
    }
}
