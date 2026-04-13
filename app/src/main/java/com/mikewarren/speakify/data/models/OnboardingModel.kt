package com.mikewarren.speakify.data.models

import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingModel(
    val appOpenCount: Int = 0,
    val onboardingStep: OnboardingUiState = OnboardingUiState.NotStarted,
    val surveyResult: String? = null,
    val primaryGoal: String? = null,
    val veryImportantApps: List<UserAppModel> = emptyList(),
)
