package com.mikewarren.speakify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.mikewarren.speakify.services.SpeakifyNotificationListener

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Ensure we are only acting on the BOOT_COMPLETED action
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device has finished booting. Ensuring NotificationListenerService is active.")
            ensureNotificationServiceIsRunning(context)
        }
    }

    /**
     * The NotificationListenerService is a bound service managed by the system.
     * By enabling and disabling its component, we can prompt the system to re-bind to it,
     * effectively restarting it and ensuring it's active after a reboot.
     */
    private fun ensureNotificationServiceIsRunning(context: Context) {
        val componentName = android.content.ComponentName(context, SpeakifyNotificationListener::class.java)
        val packageManager = context.packageManager

        // Disable and then re-enable the component. This is a well-known trick
        // to kickstart the system into re-binding to the NotificationListenerService.
        try {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("BootCompletedReceiver", "NotificationListenerService component re-enabled successfully.")
        } catch (e: Exception) {
            Log.e("BootCompletedReceiver", "Failed to re-enable NotificationListenerService", e)
        }
    }
}