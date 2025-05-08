package com.mikewarren.speakify.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat


class NotificationPermissionHelper(private val context: Context) {

    fun getAppsWithNotificationPermission(): List<ApplicationInfo> {
        val allApps = getAllApps()
        return allApps.filter { applicationInfo: ApplicationInfo ->  hasNotificationPermission(applicationInfo) }
    }

    fun getAllApps(): List<ApplicationInfo> {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            packageManager.queryIntentActivities(mainIntent, 0)
        }

        return resolveInfoList.mapNotNull { resolveInfo ->
            try {
                packageManager.getApplicationInfo(resolveInfo.activityInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                // Handle cases where the application info can't be found.
                // This could happen if the app was uninstalled.
                null
            }
        }
    }

    private fun hasNotificationPermission(applicationInfo: ApplicationInfo): Boolean {
        // Check if the app is opted into notifications
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = getMode(appOpsManager, applicationInfo)
            return mode == AppOpsManager.MODE_ALLOWED
          }

        // only returns if THIS APP has permission to send notifications
       return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun getMode(appOpsManager: AppOpsManager, applicationInfo: ApplicationInfo): Int {
        return onGetOp()(appOpsManager,
            "android:post_notification",
            applicationInfo.uid,
            applicationInfo.packageName,
            )
    }

    private fun onGetOp(): (AppOpsManager, String, Int, String) -> Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AppOpsManager::unsafeCheckOpNoThrow
        }
        return AppOpsManager::checkOpNoThrow
    }

}

