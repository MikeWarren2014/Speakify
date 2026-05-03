package com.mikewarren.speakify.activities

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.PhonePermissionEvent
import com.mikewarren.speakify.data.events.PhonePermissionEventBus
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.PhonePermissionsView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhonePermissionsActivity : BaseMutliplePermissionsActivity<PhonePermissionEvent>(
    eventBus = PhonePermissionEventBus.GetInstance(),
    permissionRequestCode = PermissionCodes.PhonePermissions,
) {

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isGranted = result.resultCode == RESULT_OK
            onPermissionResult?.invoke(isGranted)
            onPermissionResult = null
        }

    override fun onCheckPermission(permission: String) {
        if (permission == Manifest.permission.BIND_SCREENING_SERVICE) {
            requestCallScreeningRole()
            return
        }
        requestPermissionLauncher.launch(permission)
    }

    @Composable
    override fun PermissionListView(
        permissions: Array<String>,
        onRequestPermission: (String, (Boolean) -> Unit) -> Unit,
        onDone: (Boolean) -> Unit
    ) {
        PhonePermissionsView(permissions = permissions,
            onRequestPermission = onRequestPermission,
            onDone = onDone)
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

    override fun getPermissionGrantedEvent(): PhonePermissionEvent {
        return PhonePermissionEvent.PermissionGranted
    }

}
