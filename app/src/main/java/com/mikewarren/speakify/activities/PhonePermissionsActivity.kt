package com.mikewarren.speakify.activities

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
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
            val hasOverlayPermissionSynced = handleOverlayPermissionsAfterRoleGranted()

            onPermissionResult?.invoke(isGranted && hasOverlayPermissionSynced)
            onPermissionResult = null
        }

    /**
     * @return {Boolean} whether or not the OS has synchronized the grant state of the permission
     */
    private fun handleOverlayPermissionsAfterRoleGranted(): Boolean {
        if (Settings.canDrawOverlays(this)) {
            // we let the role launcher handle this
            return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
        overlayLauncher.launch(intent)
        return false
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val hasOverlay = Settings.canDrawOverlays(this)
        onPermissionResult?.invoke(hasOverlay)
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
            Manifest.permission.SYSTEM_ALERT_WINDOW,
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
