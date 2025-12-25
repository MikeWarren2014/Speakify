package com.mikewarren.speakify.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.InitialScreenView
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val viewModel: MainViewModel by viewModels()
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme by settingsViewModel.useDarkTheme.collectAsStateWithLifecycle(isSystemInDarkTheme())

            // When the state becomes SignedIn, navigate to MainActivity
            if (state is MainUiState.SignedIn) {
                LaunchedEffect(state) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }

            MyApplicationTheme(darkTheme = useDarkTheme == true, content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Use theme's background color
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            is MainUiState.Loading -> CircularProgressIndicator()
                            is MainUiState.SignedOut -> InitialScreenView()
                            is MainUiState.SignedIn -> {
                                Text("Successfully signed in. Redirecting to app...")
                            }
                            is MainUiState.AccountDeleted -> {
                                TODO("Implement prompt asking users for feedback, and a way to send that feedback.")
                            }
                        }
                    }
                }
            })
        }
    }
}
