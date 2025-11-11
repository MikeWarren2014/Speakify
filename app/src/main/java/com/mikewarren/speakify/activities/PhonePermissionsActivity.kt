package com.mikewarren.speakify.activities

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.PhonePermissionEvent
import com.mikewarren.speakify.data.events.PhonePermissionEventBus

class PhonePermissionsActivity() : BasePermissionRequesterActivity<PhonePermissionEvent>(
    eventBus = PhonePermissionEventBus.GetInstance(),
    permissionRequestCode = PermissionCodes.PhonePermissions,
) {
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // You can check here if all permissions were granted and update the UI if needed.
            if (permissions[Manifest.permission.READ_CALL_LOG] == true &&
                (permissions[Manifest.permission.READ_PHONE_STATE] == true)) {
                Log.d(this.javaClass.name, "Phone state and call log permissions granted.")
                onPermissionGranted()
                return@registerForActivityResult
            }
            Log.w(this.javaClass.name, "One or more phone permissions were denied.")
            eventBus.post(getPermissionDeniedEvent())
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    override fun getPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
        )
    }

    override fun getPermissionDeniedEvent(): PhonePermissionEvent {
        return PhonePermissionEvent.PermissionDenied
    }

    override fun getFailureEvent(message: String): PhonePermissionEvent {
        return PhonePermissionEvent.Failure(message)
    }



    override fun onPermissionGranted() {
        Log.d(this.javaClass.name, "Phone state and call log permissions granted.")
        eventBus.post(PhonePermissionEvent.PermissionGranted)
        finish()
    }

    override fun handleUngrantedPermissions(ungrantedPermissions: Array<String>) {

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Speakify needs access to your phone state and call logs to announce incoming calls. Please grant these permissions.")
            .setPositiveButton("OK") { _, _ ->
                requestMultiplePermissionsLauncher.launch(ungrantedPermissions)
            }
            .setNegativeButton("Cancel", { _, _ ->
                eventBus.post(getFailureEvent("User denied permissions"))
                finish()
            })
            .show()
    }
}