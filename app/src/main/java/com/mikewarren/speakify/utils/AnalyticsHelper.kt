package com.mikewarren.speakify.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.mikewarren.speakify.data.db.UserAppModel
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

    fun logPrimaryGoal(goal: String) {
        val bundle = Bundle().apply {
            putString("goal_name", goal)
        }
        logEvent("onboarding_goal_selected", bundle)
    }

    fun logVIAs(viaList: List<UserAppModel>) {
        if (viaList.isEmpty()) return
        
        val bundle = Bundle().apply {
            putInt("app_count", viaList.size)
            putString("apps_list", viaList.joinToString(", ") { it.appName })
        }
        logEvent("onboarding_vias_selected", bundle)
    }

    fun logTrialConversionStarted() {
        logEvent("trial_conversion_started")
    }

    fun logTrialContinued() {
        logEvent("trial_continued_to_session")
    }
}
