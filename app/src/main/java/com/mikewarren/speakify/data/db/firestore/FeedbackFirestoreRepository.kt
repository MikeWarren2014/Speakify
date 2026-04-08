package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackFirestoreRepository @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) : BaseChildFirestoreRepository() {

    override fun getSuccessLogMessage(): String = "Feedback synced successfully"
    override fun getFailureLogMessage(): String = "Failed to sync feedback"

    override suspend fun settingsTransaction(): Result<Unit> = Result.success(Unit)

    override suspend fun importantAppsTransactionList(): List<suspend () -> Result<Unit>> = emptyList()

    override suspend fun appSettingsTransactionsList(): List<suspend () -> Result<Unit>> = emptyList()

    override suspend fun recentMessengerContactsTransactionList(): List<suspend () -> Result<Unit>> = emptyList()

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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
