package com.mikewarren.speakify.activities

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.PhonePermissionEvent
import com.mikewarren.speakify.data.events.PhonePermissionEventBus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhonePermissionsActivity() : BasePermissionRequesterActivity<PhonePermissionEvent>(
    eventBus = PhonePermissionEventBus.GetInstance(),
    permissionRequestCode = PermissionCodes.PhonePermissions,
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    override fun getPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE,
        )
    }

    override fun getPermissionDeniedEvent(): PhonePermissionEvent {
        return PhonePermissionEvent.PermissionDenied
    }

    override fun getFailureEvent(message: String): PhonePermissionEvent {
        return PhonePermissionEvent.Failure(message)
    }



    override fun onPermissionGranted() {
        Log.d(this.javaClass.name, "Phone state permissions granted.")
        eventBus.post(PhonePermissionEvent.PermissionGranted)
        finish()
    }

}