package com.mikewarren.speakify.activities

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mikewarren.speakify.data.events.BaseEventBus
import com.mikewarren.speakify.data.events.NotificationPermissionEvent
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.NotificationPermissionsView


abstract class BaseMutliplePermissionsActivity<T>(
    eventBus: BaseEventBus<T>,
    permissionRequestCode: Int,
    ):
    BasePermissionRequesterActivity<T>(eventBus, permissionRequestCode){

    protected var onPermissionResult: ((Boolean) -> Unit)? = null

    protected fun createPermissionLauncher() : ActivityResultLauncher<String> {
        return registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult?.invoke(isGranted)
            onPermissionResult = null
        }
    }

    override fun doDisplay() {
        super.doDisplay()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO: replace with a permission list view
                    // TODO: create PermissionListView
                    NotificationPermissionsView(
                        permissions = getPermissions(),
                        onRequestPermission = { permission: String, onDone: (Boolean) -> Unit ->
                            onPermissionResult = onDone
                            onCheckPermission(permission)
                        },
                        onDone = { success ->
                            if (success) {
                                onPermissionGranted()
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

    protected abstract fun onCheckPermission(permission: String)

    override fun onPermissionGranted() {
        Log.d(this.javaClass.name, "All permissions granted.")
        eventBus.post(getPermissionGrantedEvent())
        finish()
    }

    abstract fun getPermissionGrantedEvent(): T

    override fun handleUngrantedPermissions(ungrantedPermissions: Array<String>) {
        // Handled via NotificationPermissionView toggles
    }
}