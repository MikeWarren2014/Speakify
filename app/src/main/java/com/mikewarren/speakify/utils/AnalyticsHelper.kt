package com.mikewarren.speakify.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logEvent(name: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(name, params)
    }

    fun logOnboardingStep(step: String) {
        val bundle = Bundle().apply {
            putString("step_name", step)
        }
        logEvent("onboarding_step_reached", bundle)
    }

    fun logSurveyResult(result: String) {
        val bundle = Bundle().apply {
            putString("survey_answer", result)
        }
        logEvent("onboarding_survey_completed", bundle)
    }

    fun logTrialConversionStarted() {
        logEvent("trial_conversion_started")
    }

    fun logTrialContinued() {
        logEvent("trial_continued_to_session")
    }
}
