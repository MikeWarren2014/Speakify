package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.FeedbackModel
import com.mikewarren.speakify.data.models.OnboardingCategorySelection
import com.mikewarren.speakify.data.models.OnboardingModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val appOpenCount: Flow<Int>
    val speakificationCount: Flow<Int>
    val onboardingStep: Flow<OnboardingUiState>
    val feedback: Flow<FeedbackModel?>
    val primaryGoal: Flow<String?>
    val importantAppCategories: Flow<List<OnboardingCategorySelection>>
    val hasShownRatingsPrompt: Flow<Boolean>
    val hasShownTrialConversionPrompt: Flow<Boolean>
    val onboardingModel: Flow<OnboardingModel>

    suspend fun incrementAppOpenCount()
    suspend fun incrementSpeakificationCount()
    suspend fun updateOnboardingStep(step: OnboardingUiState)
    suspend fun saveFeedback(feedback: FeedbackModel)
    suspend fun savePrimaryGoal(goal: String)
    suspend fun saveImportantAppCategories(categories: List<String>)
    suspend fun satisfyCategory(category: AppCategory)
    suspend fun setHasShownRatingsPrompt(shown: Boolean)
    suspend fun setHasShownTrialConversionPrompt(shown: Boolean)
    suspend fun updateRatingsPrompt(lastAsked: Long, asksCount: Int)
    suspend fun restoreOnboardingModel(model: OnboardingModel)
}