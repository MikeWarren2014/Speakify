package com.mikewarren.speakify.utils

import android.content.Context
import android.provider.Settings.Secure
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val deviceId: String
        get() = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
}
