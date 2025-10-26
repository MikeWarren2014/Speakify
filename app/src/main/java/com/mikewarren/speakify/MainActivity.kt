package com.mikewarren.speakify

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.forEach
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.key.key
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import com.mikewarren.speakify.viewsAndViewModels.AppView
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity()  {
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MainActivity", "${it.key} = ${it.value}")
            }
            // You can check here if all permissions were granted and update the UI if needed.
            if (permissions[Manifest.permission.READ_CALL_LOG] == true &&
                (permissions[Manifest.permission.READ_PHONE_STATE] == true)) {
                Log.d("MainActivity", "Phone state and call log permissions granted.")
            } else {
                Log.w("MainActivity", "One or more phone permissions were denied.")
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            viewModel.useDarkTheme.collectLatest { useDarkTheme ->
                setContent {
                    MyApplicationTheme(darkTheme = useDarkTheme == true, content = {
                        AppView()
                    })
                }
            }
        }

        // Check for permission when the activity is created
        checkAndRequestNotificationAccess()
        checkAndRequestPhonePermissions()
    }

    private fun checkAndRequestNotificationAccess() {
        // Check if the permission is already granted
        if (!NotificationPermissionHelper(this).isNotificationServiceEnabled()) {
            // Permission not granted, show a dialog to the user
            AlertDialog.Builder(this)
                .setTitle("Notification Access Required")
                .setMessage("Speakify needs access to your notifications to read them aloud. Please enable it in the upcoming settings screen.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    // Create an intent to open the notification listener settings
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun checkAndRequestPhonePermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
        )

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNotGranted.isNotEmpty()) {
            // Show a dialog explaining why you need these permissions, then request them.
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Speakify needs access to your phone state and call logs to announce incoming calls. Please grant these permissions.")
                .setPositiveButton("OK") { _, _ ->
                    requestMultiplePermissionsLauncher.launch(permissionsNotGranted.toTypedArray())
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
