package com.mikewarren.speakify.activities

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.PhonePermissionEvent
import com.mikewarren.speakify.data.events.PhonePermissionEventBus
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.PhonePermissionsView
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.PhonePermissionsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhonePermissionsActivity : BasePermissionRequesterActivity<PhonePermissionEvent>(
    eventBus = PhonePermissionEventBus.GetInstance(),
    permissionRequestCode = PermissionCodes.PhonePermissions,
) {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult?.invoke(isGranted)
            onPermissionResult = null
        }

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isGranted = result.resultCode == RESULT_OK
            onPermissionResult?.invoke(isGranted)
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
                    PhonePermissionsView(
                        permissions = getPermissions(),
                        onRequestPermission = { permission: String, onDone: (Boolean) -> Unit ->
                            onPermissionResult = onDone
                            if (permission == Manifest.permission.BIND_SCREENING_SERVICE) {
                                requestCallScreeningRole()
                            } else {
                                requestPermissionLauncher.launch(permission)
                            }
                        },
                        onDone = { success ->
                            if (success) {
                                onPermissionGranted()
                                return@PhonePermissionsView
                            }
                            eventBus.post(getPermissionDeniedEvent())
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
                return
            }
            onPermissionResult?.invoke(false)
        } else {
            onPermissionResult?.invoke(true)
        }
        onPermissionResult = null
    }

    override fun getPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.BIND_SCREENING_SERVICE,
        )
    }

    override fun getPermissionDeniedEvent(): PhonePermissionEvent {
        return PhonePermissionEvent.PermissionDenied
    }

    override fun getFailureEvent(message: String): PhonePermissionEvent {
        return PhonePermissionEvent.Failure(message)
    }

    override fun onPermissionGranted() {
        Log.d(this.javaClass.name, "Phone permissions granted.")
        eventBus.post(PhonePermissionEvent.PermissionGranted)
        finish()
    }

    override fun handleUngrantedPermissions(ungrantedPermissions: Array<String>) {
        // Handled via PhonePermissionsView toggles
    }
}
