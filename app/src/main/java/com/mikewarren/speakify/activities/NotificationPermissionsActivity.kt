package com.mikewarren.speakify.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.NotificationPermissionEvent
import com.mikewarren.speakify.data.events.NotificationPermissionEventBus
import com.mikewarren.speakify.utils.NotificationPermissionHelper

class NotificationPermissionsActivity :
    BasePermissionRequesterActivity<NotificationPermissionEvent>(
        eventBus = NotificationPermissionEventBus.GetInstance(),
        permissionRequestCode = PermissionCodes.NotificationPermissions
    ) {

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check POST_NOTIFICATIONS (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isPostGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
                if (!isPostGranted) {
                    Log.w(this.javaClass.name, "POST_NOTIFICATIONS permission denied.")
                    eventBus.post(getPermissionDeniedEvent())
                    finish()
                    return@registerForActivityResult
                }
            }

            // If runtime permissions are good, check Listener permissions
            checkListenerPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The Base class logic for requestPermissions() is slightly different here
        // because we have a mix of Runtime + System Settings permissions.
        startPermissionFlow()
    }

    private fun startPermissionFlow() {
        val permissionsToRequest = getPermissions()

        // if we are on Android 12 or below, we just check the listener permission
        if (permissionsToRequest.isEmpty()) {
            checkListenerPermission()
            return
        }

        // If we have runtime permissions to request (Android 13+), do that first
        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (ungranted.isEmpty()) {
            // Runtime permissions already granted, check listener
            checkListenerPermission()
            return
        }

        requestMultiplePermissionsLauncher.launch(ungranted)
    }

    override fun getPermissions(): Array<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        }
        return emptyArray()
    }

    private fun checkListenerPermission() {
        if (!NotificationPermissionHelper(this).isNotificationServiceEnabled()) {
            showListenerPermissionDialog()
            return
        }
        onPermissionGranted()
    }

    private fun showListenerPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_access_required_title))
            .setMessage(getString(R.string.notification_access_required_message))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                // We don't use the launcher here because Settings doesn't return a result intent cleanly
                // We rely on onActivityResult or simply checking in onRestart/onResume logic if we were standard
                // But since this is a dedicated activity, startActivityForResult is okay.
                startActivityForResult(intent, REQUEST_CODE_LISTENER_SETTINGS)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                eventBus.post(getPermissionDeniedEvent())
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_LISTENER_SETTINGS) {
            return
        }
        if (NotificationPermissionHelper(this).isNotificationServiceEnabled()) {
            onPermissionGranted()
            return
        }
        // User came back without enabling it
        eventBus.post(getPermissionDeniedEvent())
        finish()
    }

    override fun onPermissionGranted() {
        Log.d(this.javaClass.name, "All notification permissions granted.")
        eventBus.post(NotificationPermissionEvent.PermissionGranted)
        finish()
    }

    override fun getPermissionDeniedEvent(): NotificationPermissionEvent {
        return NotificationPermissionEvent.PermissionDenied
    }

    override fun getFailureEvent(message: String): NotificationPermissionEvent {
        return NotificationPermissionEvent.Failure(message)
    }

    override fun handleUngrantedPermissions(ungrantedPermissions: Array<String>) {
        // This is called by BasePermissionRequester if the initial runtime request fails
        // or if we manually trigger it.
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permissions_required_title))
            .setMessage(getString(R.string.permissions_required_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                requestMultiplePermissionsLauncher.launch(ungrantedPermissions)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                eventBus.post(getFailureEvent("User denied permissions"))
                finish()
            }
            .show()
    }

    companion object {
        private const val REQUEST_CODE_LISTENER_SETTINGS = 2002
    }
}
