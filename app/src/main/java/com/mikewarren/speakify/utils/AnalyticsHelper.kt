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
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_LIST_NAME, "onboarding_primary_goals")
            param(FirebaseAnalytics.Param.ITEMS, arrayOf(
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, goal)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, goal)
                }
            ))
        }
    }

    fun logVIAs(viaList: List<UserAppModel>) {
        if (viaList.isEmpty()) return
        
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_LIST_NAME, "very_important_apps")
            param(FirebaseAnalytics.Param.ITEMS, 
                viaList.map { item ->
                    Bundle().apply {
                        putString(FirebaseAnalytics.Param.ITEM_ID, item.packageName)
                        putString(FirebaseAnalytics.Param.ITEM_NAME, item.appName)
                        putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "onboarding_via")
                    }
                }.toTypedArray())
        }
    }

    fun logTrialConversionStarted() {
        logEvent("trial_conversion_started")
    }

    fun logTrialContinued() {
        logEvent("trial_continued_to_session")
    }
}
