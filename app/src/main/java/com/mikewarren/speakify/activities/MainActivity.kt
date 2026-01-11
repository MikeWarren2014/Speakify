package com.mikewarren.speakify.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.ActionConstants
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import com.mikewarren.speakify.viewsAndViewModels.AppView
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity()  {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]


        lifecycleScope.launch {
            viewModel.useDarkTheme.collectLatest { useDarkTheme ->
                setContent {
                    val state by viewModel.childMainVM.uiState.collectAsStateWithLifecycle()

                    val accountDeletionUiState by viewModel.childMainVM.accountDeletionUiState.collectAsStateWithLifecycle()

                    if (state is MainUiState.SignedOut) {
                        LaunchedEffect(state) {
                            Log.d("MainActivity", "Signing out and going to the LoginActivity from the MainActivity itself...")


                            val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                if (accountDeletionUiState is AccountDeletionUiState.Deleted) {
                                    viewModel.childMainVM.cancelAccountDeletion()
                                    return@apply
                                }
                                if (accountDeletionUiState !is AccountDeletionUiState.NotRequested) {
                                    putExtra(
                                        ActionConstants.PostLoginActionKey,
                                        ActionConstants.ActionDeleteAccount
                                    )
                                }
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

        lifecycleScope.launch {
            val selectedTTSVoice = settingsRepository.selectedTTSVoice.first()
            if (selectedTTSVoice.isNullOrEmpty()) {
                settingsRepository.saveSelectedVoice(Constants.DefaultTTSVoice)
            }

        }

        checkPermissions()

    }

    private fun checkPermissions() {
        val helper = NotificationPermissionHelper(this)
        val needsListener = !helper.isNotificationServiceEnabled()

        // Check Post Notifications (Android 13+)
        val needsPost = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)

        if (needsListener || needsPost) {
            val intent = Intent(this, NotificationPermissionsActivity::class.java)
            startActivity(intent)
        }
    }



}