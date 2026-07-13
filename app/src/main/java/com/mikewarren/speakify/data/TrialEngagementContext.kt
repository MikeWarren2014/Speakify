package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.models.FeedbackModel
import com.mikewarren.speakify.data.models.RatingsPromptModel
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState

sealed interface TrialEngagementContext {
    val trialModel: TrialModel?
    val onboardingStep: OnboardingUiState
    val speakificationCount: Int
    val openCount: Int
    val hasShownRatingsPrompt: Boolean
    val hasShownTrialConversionPrompt: Boolean
    val ratingsPrompt: RatingsPromptModel
    val feedback: FeedbackModel?

    data class Active(
        override val trialModel: TrialModel,
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean,
        override val ratingsPrompt: RatingsPromptModel,
        override val feedback: FeedbackModel?
    ) : TrialEngagementContext, PromptController, OnboardingController {
        override fun shouldShowTrialConversionPrompt(): Boolean = !hasShownTrialConversionPrompt && (speakificationCount >= 5 || openCount >= 3)
    }

    data class Loading(
        override val trialModel: TrialModel,
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean,
        override val ratingsPrompt: RatingsPromptModel,
        override val feedback: FeedbackModel?
    ) : TrialEngagementContext

    data class TrialBypass(
        override val onboardingStep: OnboardingUiState,
        override val speakificationCount: Int,
        override val openCount: Int,
        override val hasShownRatingsPrompt: Boolean,
        override val hasShownTrialConversionPrompt: Boolean,
        override val ratingsPrompt: RatingsPromptModel,
        override val feedback: FeedbackModel?
    ) : TrialEngagementContext, PromptController, OnboardingController {
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
        override val hasShownTrialConversionPrompt: Boolean,
        override val ratingsPrompt: RatingsPromptModel,
        override val feedback: FeedbackModel?
    ) : TrialEngagementContext

    companion object {
        fun from(
            trialModel: TrialModel,
            onboardingStep: OnboardingUiState,
            speakificationCount: Int,
            openCount: Int,
            hasShownRatingsPrompt: Boolean,
            hasShownTrialConversionPrompt: Boolean,
            ratingsPrompt: RatingsPromptModel,
            feedback: FeedbackModel?
        ): TrialEngagementContext {
            return when (trialModel.status) {
                is TrialStatus.Active -> Active(
                    trialModel,
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt,
                    ratingsPrompt,
                    feedback
                )

                is TrialStatus.NotNeeded -> TrialBypass(
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt,
                    ratingsPrompt,
                    feedback
                )

                is TrialStatus.Loading -> Loading(
                    trialModel,
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt,
                    ratingsPrompt,
                    feedback
                )

                else -> Other(
                    trialModel,
                    onboardingStep,
                    speakificationCount,
                    openCount,
                    hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt,
                    ratingsPrompt,
                    feedback
                )
            }
        }
    }
}

interface PromptController {
    val hasShownRatingsPrompt: Boolean
    val ratingsPrompt: RatingsPromptModel
    val speakificationCount: Int

    fun shouldShowRatingsPrompt(): Boolean {
        if (hasShownRatingsPrompt) return false
        if (speakificationCount < 1) return false

        val lastAsked = ratingsPrompt.lastAskedForReview ?: return true
        val asksCount = ratingsPrompt.numberOfReviewAsks

        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
        val enoughTimePassed = System.currentTimeMillis() - lastAsked > oneWeekMillis

        return enoughTimePassed && asksCount < 3
    }
    fun shouldShowTrialConversionPrompt(): Boolean
}

interface OnboardingController {
    val onboardingStep: OnboardingUiState

    fun shouldShowOnboarding(): Boolean = onboardingStep != OnboardingUiState.Completed
}
