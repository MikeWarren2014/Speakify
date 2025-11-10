package com.mikewarren.speakify.activities

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.mikewarren.speakify.data.events.BaseEventBus

import android.os.Bundle
import androidx.core.content.ContextCompat

abstract class BasePermissionRequesterActivity<Event>(
    protected val eventBus: BaseEventBus<Event>,
    protected val permissionRequestCode : Int,
): AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        doDisplay()

        requestPermissions()

    }

    open fun doDisplay() {
        supportActionBar?.hide()
    }

    protected fun requestPermissions() {
        val ungrantedPermissions = getUngrantedPermissions().toTypedArray()
        if (ungrantedPermissions.isEmpty()) {
            onPermissionGranted()
            return
        }

        handleUngrantedPermissions(ungrantedPermissions)
    }

    protected open fun handleUngrantedPermissions(ungrantedPermissions: Array<String>) {
        requestPermissions(
            ungrantedPermissions,
            permissionRequestCode
        )
    }


    protected fun getUngrantedPermissions(): List<String> {

        return this.getPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission,
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != permissionRequestCode)
            return

        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onPermissionGranted()
            return
        }
        eventBus.post(getPermissionDeniedEvent())
        finish()
    }

    abstract fun onPermissionGranted()

    abstract fun getPermissions(): Array<String>

    abstract fun getPermissionDeniedEvent() : Event

    abstract fun getFailureEvent(message: String) : Event
}