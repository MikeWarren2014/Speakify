package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState

sealed interface TrialEngagementContext {
    val trialModel: TrialModel?
    val onboardingStep: OnboardingUiState
    val speakificationCount: Int
    val openCount: Int
    val hasShownRatingsPrompt: Boolean
    val hasShownTrialConversionPrompt: Boolean

    data class Active(
        override val trialModel: TrialModel,
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean
    ) : TrialEngagementContext, TrialPromptController, OnboardingController {
        override fun shouldShowRatingsPrompt(): Boolean = !hasShownRatingsPrompt && speakificationCount >= 1
        override fun shouldShowTrialConversionPrompt(): Boolean = !hasShownTrialConversionPrompt && (speakificationCount >= 5 || openCount >= 3)
    }

    data class Loading(
        override val trialModel: TrialModel,
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean
    ) : TrialEngagementContext

    data class TrialBypass(
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean
    ) : TrialEngagementContext, TrialPromptController, OnboardingController {
        override fun shouldShowRatingsPrompt(): Boolean = !hasShownRatingsPrompt && speakificationCount >= 1
        override fun shouldShowTrialConversionPrompt(): Boolean = false
        override val trialModel: TrialModel?
            get() = null
    }

    data class Other(
        override val trialModel: TrialModel,
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean
    ) : TrialEngagementContext

    companion object {
        fun from(
            trialModel: TrialModel,
            onboardingStep: OnboardingUiState,
            speakificationCount: Int,
            openCount: Int,
            hasShownRatingsPrompt: Boolean,
            hasShownTrialConversionPrompt: Boolean
        ): TrialEngagementContext {
            return when (trialModel.status) {
                is TrialStatus.Active -> Active(
                    trialModel,
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt
                )

                is TrialStatus.NotNeeded -> TrialBypass(
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt,
                )

                is TrialStatus.Loading -> Loading(
                    trialModel,
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt
                )

                else -> Other(
                    trialModel,
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt
                )
            }
        }
    }
}

interface TrialPromptController {
    fun shouldShowRatingsPrompt(): Boolean
    fun shouldShowTrialConversionPrompt(): Boolean
}

interface OnboardingController {
    val onboardingStep: OnboardingUiState

    fun shouldShowOnboarding(): Boolean = onboardingStep != OnboardingUiState.Completed
}
