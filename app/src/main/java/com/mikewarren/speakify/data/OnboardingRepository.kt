package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.OnboardingCategorySelection
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val appOpenCount: Flow<Int>
    val onboardingStep: Flow<OnboardingUiState>
    val surveyResult: Flow<String?>
    val primaryGoal: Flow<String?>
    val importantAppCategories: Flow<List<OnboardingCategorySelection>>
    val veryImportantApps: Flow<List<UserAppModel>>

    suspend fun incrementAppOpenCount()
    suspend fun updateOnboardingStep(step: OnboardingUiState)
    suspend fun saveSurveyResult(result: String)
    suspend fun savePrimaryGoal(goal: String)
    suspend fun saveImportantAppCategories(categories: List<String>)
    suspend fun satisfyCategory(category: AppCategory)
    suspend fun saveVeryImportantApps(vias: List<UserAppModel>)
}