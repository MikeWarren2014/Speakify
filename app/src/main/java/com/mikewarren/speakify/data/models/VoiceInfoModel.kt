package com.mikewarren.speakify.data.models

// A simple data class to hold structured information about a TTS voice.
data class VoiceInfoModel(
    val name: String,         // The unique, raw name (e.g., "en-us-x-sfg#male_1-local")
    val displayName: String,  // The user-friendly display name (e.g., "English (United States)")
    val language: String,     // e.g., "en"
    val country: String       // e.g., "USA"
)