package com.mikewarren.speakify.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.activities.MainActivity
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainUiState
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.SignInOrUpView


class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel by viewModels()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // When the state becomes SignedIn, navigate to MainActivity
            if (state is MainUiState.SignedIn) {
                LaunchedEffect(state) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is MainUiState.Loading -> CircularProgressIndicator()
                    is MainUiState.SignedOut -> SignInOrUpView()
                    is MainUiState.SignedIn -> {
                        Text("Successfully signed in. Redirecting...")
                    }
                }
            }
        }
    }
}
