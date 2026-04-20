package com.mikewarren.speakify.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.NotificationPermissionEvent
import com.mikewarren.speakify.data.events.NotificationPermissionEventBus
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.NotificationPermissionsView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationPermissionsActivity :
    BasePermissionRequesterActivity<NotificationPermissionEvent>(
        eventBus = NotificationPermissionEventBus.GetInstance(),
        permissionRequestCode = PermissionCodes.NotificationPermissions
    ) {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult?.invoke(isGranted)
            onPermissionResult = null
        }

    private val listenerSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val isEnabled = NotificationPermissionHelper(this).isNotificationServiceEnabled()
            onPermissionResult?.invoke(isEnabled)
            onPermissionResult = null
        }

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    override fun doDisplay() {
        super.doDisplay()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotificationPermissionsView(
                        permissions = getPermissions(),
                        onRequestPermission = { permission: String, onDone: (Boolean) -> Unit ->
                            onPermissionResult = onDone
                            if (permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                listenerSettingsLauncher.launch(intent)
                            } else {
                                requestPermissionLauncher.launch(permission)
                            }
                        },
                        onDone = { success ->
                            if (success) {
                                allPermissionsGranted()
                                return@NotificationPermissionsView
                            }
                            eventBus.post(getPermissionDeniedEvent())
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun getPermissions(): Array<String> {
        return NotificationPermissionsActivity.getPermissions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPermissionGranted() {
        // Wait for user to interact with the view
        // NOTE: this is disabled in favor of the NotificationPermissionView

    }

    private fun allPermissionsGranted() {
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
        // Handled via NotificationPermissionView toggles
    }

    companion object {
        private const val REQUEST_CODE_LISTENER_SETTINGS = 2002

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