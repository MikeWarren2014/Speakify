package com.mikewarren.speakify

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
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
}
