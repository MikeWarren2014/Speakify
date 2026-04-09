package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.BaseUserFirestoreRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.utils.log.IResultLoggable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackFirestoreRepository @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) : BaseUserFirestoreRepository(), IResultLoggable {
    override fun getSuccessLogMessage(): String = "Feedback synced successfully"
    override fun getFailureLogMessage(): String = "Failed to sync feedback"


    suspend fun syncFeedback(): Result<Unit> {
        val onboardingStep = onboardingRepository.onboardingStep.first()
        val surveyResultValue = onboardingRepository.surveyResult.first()

        val feedbackData = hashMapOf(
            "onboardingStep" to onboardingStep.toString(),
            "surveyResult" to surveyResultValue,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        return try {
            safeFirestoreCall {
                userDoc.collection("feedback")
                    .document("onboarding")
                    .set(feedbackData)
                    .await()
            }
            logSuccessResult()
            Result.success(Unit)
        } catch (e: Exception) {
            logFailureResult(e)
            Result.failure(e)
        }
    }
}
