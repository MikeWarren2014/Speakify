package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val appOpenCount: Flow<Int>
    val onboardingStep: Flow<OnboardingUiState>
    val surveyResult: Flow<String?>
    val primaryGoal: Flow<String?>
    val veryImportantApps: Flow<List<UserAppModel>>

    suspend fun incrementAppOpenCount()
    suspend fun updateOnboardingStep(step: OnboardingUiState)
    suspend fun saveSurveyResult(result: String)
    suspend fun savePrimaryGoal(goal: String)
    suspend fun saveVeryImportantApps(vias: List<UserAppModel>)
}