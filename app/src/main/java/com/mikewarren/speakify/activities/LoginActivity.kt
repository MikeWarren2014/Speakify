package com.mikewarren.speakify.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.constants.ActionConstants
import com.mikewarren.speakify.data.uiStates.InitialScreenUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.InitialScreenView
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.SignInOrUpView
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.TrialActiveView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel: MainViewModel by viewModels()

        if (savedInstanceState == null) {
            Log.d("LoginActivity", "onCreate called with savedInstanceState null")
            viewModel.resetTrialAuthorized()
        }

        setContent {
            val settingsViewModel: SettingsViewModel by viewModels()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme by settingsViewModel.useDarkTheme.collectAsStateWithLifecycle(initialValue = null)

            // When the state becomes SignedIn, navigate to MainActivity
            if (state is MainUiState.SignedIn) {
                LaunchedEffect(state) {
                    handleLoginSuccess(intent.getStringExtra(ActionConstants.PostLoginActionKey), viewModel)
                }
            }

            // When the state becomes TrialUsage, navigate to MainActivity
            if (state is MainUiState.TrialUsage) {
                LaunchedEffect(state) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }

            MyApplicationTheme(darkTheme = useDarkTheme ?: isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            is MainUiState.Loading -> CircularProgressIndicator()
                            is MainUiState.SignedOut ->  InitialScreenView()
                            is MainUiState.SignedIn -> {
                                Text(getString(R.string.signed_in),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            MainUiState.TrialActive -> TrialActiveView()

                            MainUiState.TrialUsage -> {
                                Text(
                                    stringResource(R.string.trial_usage_redirect),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            MainUiState.TrialConversion -> SignInOrUpView(InitialScreenUiState.SignUp)

                            MainUiState.TrialEnded -> InitialScreenView()

                        }
                    }
                }
            }
        }
    }

    private fun handleLoginSuccess(postLoginAction: String?, viewModel: MainViewModel) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (postLoginAction == ActionConstants.ActionDeleteAccount) {
                viewModel.markAccountVerified()

                putExtra(ActionConstants.PostLoginActionKey, ActionConstants.ActionDeleteAccount)
            }
        })
    }
}
