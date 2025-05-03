package com.mikewarren.speakify.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat


class NotificationPermissionHelper(private val context: Context) {

    fun getAppsWithNotificationPermission(): List<ApplicationInfo> {
        val packageManager = context.packageManager
        val installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return installedApplications.filter { hasNotificationPermission(it.packageName) }
    }

    private fun hasNotificationPermission(packageName: String): Boolean {
        // Check if the app is opted into notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = getMode(appOpsManager, packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }

        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun getMode(appOpsManager: AppOpsManager, packageName: String): Int {
        return onGetOp()(appOpsManager,
            "android:post_notification",
            android.os.Process.myUid(),
            packageName,
            )
    }

    private fun onGetOp(): (AppOpsManager, String, Int, String) -> Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AppOpsManager::unsafeCheckOpNoThrow
        }
        return AppOpsManager::checkOpNoThrow
    }

}

