package com.mikewarren.speakify.utils

import android.content.Context
import android.content.pm.ApplicationInfo

class AppNameHelper(private val context: Context) {
    val packageManager = context.packageManager

    fun getAppDisplayName(appPackageName: String): String {
        return getAppDisplayName(packageManager.getApplicationInfo(appPackageName, 0))

    }

    fun getAppDisplayName(appInfo: ApplicationInfo): String {
        return packageManager.getApplicationLabel(appInfo).toString()
    }
}