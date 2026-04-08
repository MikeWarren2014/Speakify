package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val appOpenCount: Flow<Int>
    val onboardingStep: Flow<OnboardingUiState>
    val surveyResult: Flow<String?>

    suspend fun incrementAppOpenCount()
    suspend fun updateOnboardingStep(step: OnboardingUiState)
    suspend fun saveSurveyResult(result: String)
}