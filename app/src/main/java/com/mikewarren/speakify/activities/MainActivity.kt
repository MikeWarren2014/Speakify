package com.mikewarren.speakify.activities

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mikewarren.speakify.data.MainUiState
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
                    val state by viewModel.childMainVM.uiState.collectAsStateWithLifecycle()

                    if (state is MainUiState.SignedOut) {
                        LaunchedEffect(state) {
                            val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }
                    }

                    MyApplicationTheme(darkTheme = useDarkTheme == true, content = {
                        when (state) {
                            is MainUiState.Loading -> CircularProgressIndicator()
                            is MainUiState.SignedOut -> Text("Successfully signed out. Redirecting back to login page...")
                            is MainUiState.SignedIn -> {
                                AppView()
                            }
                        }

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