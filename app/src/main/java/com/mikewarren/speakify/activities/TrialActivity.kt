package com.mikewarren.speakify.activities

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.data.uiStates.InitialScreenUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.InitialScreenView
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.SignInOrUpView
import com.mikewarren.speakify.viewsAndViewModels.pages.trialOnboarding.ConversionReady
import com.mikewarren.speakify.viewsAndViewModels.pages.trialOnboarding.OnboardingView
import com.mikewarren.speakify.viewsAndViewModels.pages.trialOnboarding.SatisfactionSurvey
import com.mikewarren.speakify.viewsAndViewModels.pages.trialOnboarding.TrialActiveView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrialActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by viewModels()

        setContent {
            val settingsViewModel: SettingsViewModel by viewModels()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme by settingsViewModel.useDarkTheme.collectAsStateWithLifecycle(initialValue = null)

            // When the state becomes SignedIn or TrialUsage, navigate to MainActivity
            if (state is MainUiState.SignedIn || state is MainUiState.TrialUsage) {
                LaunchedEffect(state) {
                    startActivity(Intent(this@TrialActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }

            // When the state becomes SignedOut, navigate to LoginActivity
            if (state is MainUiState.SignedOut) {
                LaunchedEffect(state) {
                    startActivity(Intent(this@TrialActivity, LoginActivity::class.java).apply {
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
                            
                            MainUiState.TrialActive -> TrialActiveView()

                            is MainUiState.Onboarding -> {
                                val onboardingState = state as MainUiState.Onboarding
                                OnboardingView(onboardingState.step)
                            }

                            MainUiState.TrialConversion -> SignInOrUpView(InitialScreenUiState.SignUp)

                            MainUiState.TrialConversionPrompt -> ConversionReady(
                                onSignUp = { viewModel.startTrialConversion() },
                                onLater = { viewModel.proceedToTrialSession() }
                            )

                            is MainUiState.RatingsPrompt -> {
                                val ratingsState = state as MainUiState.RatingsPrompt
                                SatisfactionSurvey(
                                    initialFeedback = ratingsState.feedback,
                                    onResult = { result ->
                                        viewModel.saveFeedback(result)
                                        if (result.action != "Rate Later") {
                                            viewModel.markRatingsPromptShown()
                                        }
                                        viewModel.proceedToTrialSession()
                                    },
                                    onReviewAsk = { viewModel.recordRatingsPromptAsk() }
                                )
                            }

                            MainUiState.TrialEnded -> InitialScreenView()
                            
                            else -> {
                                // For states handled by other activities, show loading until redirect
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
