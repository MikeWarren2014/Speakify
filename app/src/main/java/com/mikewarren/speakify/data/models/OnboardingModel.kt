package com.mikewarren.speakify.data.models

import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingCategorySelection(
    val category: AppCategory,
    val isSatisfied: Boolean = false
)

@Serializable
data class OnboardingModel(
    val appOpenCount: Int = 0,
    val speakificationCount: Int = 0,
    val onboardingStep: OnboardingUiState = OnboardingUiState.NotStarted,
    val feedback: FeedbackModel? = null,
    val primaryGoal: String? = null,
    val importantAppCategories: List<OnboardingCategorySelection> = emptyList(),
    val veryImportantApps: List<UserAppModel> = emptyList(),
    val hasShownRatingsPrompt: Boolean = false,
    val hasShownTrialConversionPrompt: Boolean = false,
)
