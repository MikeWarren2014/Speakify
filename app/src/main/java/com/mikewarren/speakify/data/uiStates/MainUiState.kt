package com.mikewarren.speakify.data.uiStates

import com.mikewarren.speakify.data.models.FeedbackModel

sealed interface MainUiState {
    data object Loading : MainUiState
    data object SignedIn : MainUiState
    data object TrialActive : MainUiState

    data class Onboarding(val step: OnboardingUiState) : MainUiState

    data object TrialUsage: MainUiState
    data object TrialEnded: MainUiState
    data object SignedOut : MainUiState
    data object TrialConversion: MainUiState
    data object TrialConversionPrompt: MainUiState
    data class RatingsPrompt(val feedback: FeedbackModel?): MainUiState
}
