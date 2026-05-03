package com.mikewarren.speakify.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.NotificationPermissionEvent
import com.mikewarren.speakify.data.events.NotificationPermissionEventBus
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.NotificationPermissionsView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationPermissionsActivity :
    BaseMutliplePermissionsActivity<NotificationPermissionEvent>(
        eventBus = NotificationPermissionEventBus.GetInstance(),
        permissionRequestCode = PermissionCodes.NotificationPermissions
    ) {

    private val listenerSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val isEnabled = NotificationPermissionHelper(this).isNotificationServiceEnabled()
            onPermissionResult?.invoke(isEnabled)
            onPermissionResult = null
        }


    @Composable
    override fun PermissionListView(
        permissions: Array<String>,
        onRequestPermission: (String, (Boolean) -> Unit) -> Unit,
        onDone: (Boolean) -> Unit
    ) {
        NotificationPermissionsView(permissions = permissions,
            onRequestPermission = onRequestPermission,
            onDone = onDone)
    }

    override fun onCheckPermission(permission: String) {
        if (permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            listenerSettingsLauncher.launch(intent)
            return
        }
        requestPermissionLauncher.launch(permission)
    }

    override fun getPermissions(): Array<String> {
        return NotificationPermissionsActivity.getPermissions()
    }

    override fun getPermissionGrantedEvent(): NotificationPermissionEvent {
        return NotificationPermissionEvent.PermissionGranted
    }


    override fun getPermissionDeniedEvent(): NotificationPermissionEvent {
        return NotificationPermissionEvent.PermissionDenied
    }

    override fun getFailureEvent(message: String): NotificationPermissionEvent {
        return NotificationPermissionEvent.Failure(message)
    }

    companion object {

        fun getPermissions(): Array<String> {
            val basePermissions: Array<String> = arrayOf(
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return basePermissions + (Manifest.permission.POST_NOTIFICATIONS)
            }
            return basePermissions
        }
    }
}