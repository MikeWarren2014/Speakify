package com.mikewarren.speakify.data.uiStates

import kotlinx.serialization.Serializable

@Serializable
sealed interface OnboardingUiState {
    @Serializable
    data object NotStarted : OnboardingUiState
    @Serializable
    data object PreferenceGathering : OnboardingUiState
    @Serializable
    data object AppUsageInsight : OnboardingUiState
    @Serializable
    data object ValueDiscovery : OnboardingUiState
    @Serializable
    data object Completed : OnboardingUiState

    companion object {
        fun fromString(value: String?): OnboardingUiState {
            return when (value) {
                PreferenceGathering::class.simpleName -> PreferenceGathering
                AppUsageInsight::class.simpleName -> AppUsageInsight
                ValueDiscovery::class.simpleName -> ValueDiscovery
                Completed::class.simpleName -> Completed
                else -> NotStarted
            }
        }
    }
}
