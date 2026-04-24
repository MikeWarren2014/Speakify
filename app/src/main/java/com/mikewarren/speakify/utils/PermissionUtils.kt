package com.mikewarren.speakify.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mikewarren.speakify.utils.NotificationExtractionUtils

object PermissionUtils {
    // Function to check if a specific permission is granted
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        // Use `ContextCompat.checkSelfPermission` to verify if the permission is granted
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Function to check if all permissions in a list are granted
    fun areAllPermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            if (it != Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                return@all isPermissionGranted(context, it)
            return@all NotificationPermissionHelper(context).isNotificationServiceEnabled()
        }
    }

    // Function to determine if a rationale should be shown for a specific permission
    fun shouldShowRationale(context: Context, permission: String): Boolean {
        // Use `ActivityCompat.shouldShowRequestPermissionRationale` to check if the user
        // has previously denied the permission and might benefit from an explanation
        return ActivityCompat.shouldShowRequestPermissionRationale(
            (context as Activity), permission
        )
    }

    // Function to determine if a rationale should be shown for any permission in a list
    fun shouldShowRationaleForMultiplePermissions(context: Context, permissions: List<String>): Boolean {
        // Use the `any` function to check if a rationale should be shown for at least one permission
        return permissions.any { shouldShowRationale(context, it) }
    }
}