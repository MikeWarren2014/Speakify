package com.mikewarren.speakify.utils

import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager

object PackageHelper {
    fun GetDefaultDialerApp(context: Context): String? {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return telecomManager.defaultDialerPackage
    }
}