package com.mikewarren.speakify.data

object Constants {
    const val OneSecond: Long = 1000

    val PhoneAppPackageNames = listOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
    )

    val MessagingAppPackageNames = listOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.samsung.android.messaging",
    )

    const val DefaultTTSLanguage = "en-US-language"
    const val AutoCompleteListSizeLimit = 5
}