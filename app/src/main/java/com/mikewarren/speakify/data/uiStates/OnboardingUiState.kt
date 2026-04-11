package com.mikewarren.speakify.data.uiStates

import kotlinx.serialization.Serializable

@Serializable
sealed interface OnboardingUiState {
    @Serializable
    data object NotStarted : OnboardingUiState
    @Serializable
    data object SatisfactionSurvey : OnboardingUiState
    @Serializable
    data object PreferenceGathering : OnboardingUiState
    @Serializable
    data object AppUsageInsight : OnboardingUiState
    @Serializable
    data object ConversionReady : OnboardingUiState
    @Serializable
    data object Completed : OnboardingUiState
}
