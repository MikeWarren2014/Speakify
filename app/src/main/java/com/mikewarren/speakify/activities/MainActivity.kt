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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.ActionConstants
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import com.mikewarren.speakify.utils.PermissionUtils
import com.mikewarren.speakify.viewsAndViewModels.AppView
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity()  {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            val useDarkTheme by viewModel.useDarkTheme.collectAsStateWithLifecycle(initialValue = null)
            val state by viewModel.childMainVM.uiState.collectAsStateWithLifecycle()
            val accountDeletionUiState by viewModel.childMainVM.accountDeletionUiState.collectAsStateWithLifecycle()

            if (state is MainUiState.SignedOut) {
                LaunchedEffect(state) {
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

            if (state is MainUiState.TrialEnded) {
                LaunchedEffect(state) {
                    delay(2000)
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }

            if (state is MainUiState.TrialActive || state is MainUiState.TrialConversion) {
                LaunchedEffect(state) {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }

            MyApplicationTheme(darkTheme = useDarkTheme ?: isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (state) {
                        is MainUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is MainUiState.SignedOut -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(getString(R.string.signed_out),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        is MainUiState.SignedIn -> {
                            AppView()
                        }

                        MainUiState.TrialActive -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        MainUiState.TrialUsage -> {
                            AppView()
                        }
                        MainUiState.TrialEnded -> {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.trial_ended_main_activity),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        MainUiState.TrialConversion -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is MainUiState.Onboarding -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
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
        val permissions = NotificationPermissionsActivity.getPermissions()
        
        if (!PermissionUtils.areAllPermissionsGranted(this, permissions)) {
            val intent = Intent(this, NotificationPermissionsActivity::class.java)
            startActivity(intent)
        }
    }
}
