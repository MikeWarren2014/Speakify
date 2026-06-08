package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.utils.log.LogUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackFirestoreRepository @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) : BaseMultipleFirestoreTransactionsRepository() {
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

        val syncTransaction = suspend {
            val result = writeTransaction(userDoc.collection("feedback")
                .document("onboarding"),
                feedbackData)

            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                if ((exception != null) &&
                    (exception is IllegalStateException && exception.message?.contains("User not logged in") == true)) {
                    LogUtils.LogWarning(TAG, "Feedback sync skipped: User not logged in to Firebase.")
                } else {
                    logFailureResult(exception as Exception)
                }
            }
            result
        }

        return doFirestoreTransactions(listOf(this::writeClerkUserData, syncTransaction))
    }
}
