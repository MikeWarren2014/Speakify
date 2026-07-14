package com.mikewarren.speakify.data.db.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.FeedbackModel
import com.mikewarren.speakify.data.models.OnboardingCategorySelection
import com.mikewarren.speakify.data.models.OnboardingModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,
    private val onboardingRepository: OnboardingRepository,
): BaseChildFirestoreRepository() {
    override fun getSuccessLogMessage(): String {
        return "All data restored successfully for user $userId"
    }

    override fun getFailureLogMessage(): String {
        return "Failed to restore data"
    }

    override suspend fun settingsTransaction(): Result<Unit> {
        val settingsSnapshot = safeFirestoreCall {
            userDoc.collection("config")
                .document("settings")
                .get()
                .await()
        }
        
        if (!settingsSnapshot.exists()) {
            return Result.success(Unit)
        }

        return transaction(settingsSnapshot) { documentSnapshot ->
            documentSnapshot.getBoolean("useDarkTheme")?.let {
                settingsRepository.updateUseDarkTheme(it)
            }
            documentSnapshot.getString("selectedTTSVoice")?.let {
                settingsRepository.saveSelectedVoice(it)
            }
            documentSnapshot.getBoolean("maximizeVolumeOnScreenOff")?.let {
                settingsRepository.setMaximizeVolumeOnScreenOff(it)
            }
            documentSnapshot.getLong("minVolume")?.let {
                settingsRepository.setMinVolume(it.toInt())
            }
            documentSnapshot.getBoolean("isCrashlyticsEnabled")?.let {
                settingsRepository.setCrashlyticsEnabled(it)
            }

        }
    }

    override suspend fun onboardingTransaction(): Result<Unit> {
        val onboardingSnapshot = safeFirestoreCall {
            userDoc
                .collection("onboarding")
                .document("onboarding")
                .get()
                .await()
        }

        if (!onboardingSnapshot.exists()) {
            return Result.success(Unit)
        }

        return transaction(onboardingSnapshot) { doc ->
            val onboardingStep = OnboardingUiState.fromString(doc.getString("onboardingStep"))
            val appOpenCount = doc.getLong("appOpenCount")?.toInt() ?: 0
            val speakificationCount = doc.getLong("speakificationCount")?.toInt() ?: 0
            val surveyResultFallback = doc.getString("surveyResult")
            val primaryGoal = doc.getString("primaryGoal")
            val hasShownRatingsPrompt = doc.getBoolean("hasShownRatingsPrompt") ?: false
            val hasShownTrialConversionPrompt = doc.getBoolean("hasShownTrialConversionPrompt") ?: false
            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time

            @Suppress("UNCHECKED_CAST")
            val importantAppCategories = (doc.get("importantAppCategories") as? List<Map<String, Any>>)?.mapNotNull { map ->
                val categoryStr = map["category"] as? String ?: return@mapNotNull null
                val isSatisfied = map["isSatisfied"] as? Boolean ?: false
                try {
                    OnboardingCategorySelection(AppCategory.valueOf(categoryStr), isSatisfied)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            onboardingRepository.restoreOnboardingModel(
                OnboardingModel(
                    appOpenCount = appOpenCount,
                    speakificationCount = speakificationCount,
                    onboardingStep = onboardingStep,
                    feedback = surveyResultFallback?.let { FeedbackModel(surveyResult = it) },
                    primaryGoal = primaryGoal,
                    importantAppCategories = importantAppCategories,
                    hasShownRatingsPrompt = hasShownRatingsPrompt,
                    hasShownTrialConversionPrompt = hasShownTrialConversionPrompt,
                    timestamp = timestamp
                )
            )
        }
    }

    override suspend fun feedbackTransaction(): Result<Unit> {
        val feedbackSnapshot = safeFirestoreCall {
            userDoc.collection("onboarding")
                .document("feedback")
                .get()
                .await()
        }

        if (!feedbackSnapshot.exists()) {
            return Result.success(Unit)
        }

        return transaction(feedbackSnapshot) { doc ->
            val surveyResult = doc.getString("surveyResult")
            val action = doc.getString("action")
            if (surveyResult != null || action != null) {
                onboardingRepository.saveFeedback(
                    FeedbackModel(surveyResult, action)
                )
            }
        }
    }

    override suspend fun ratingsPromptTransaction(): Result<Unit> {
        val ratingsPromptSnapshot = safeFirestoreCall {
            userDoc.collection("onboarding")
                .document("ratingsPrompt")
                .get()
                .await()
        }

        if (!ratingsPromptSnapshot.exists()) {
            return Result.success(Unit)
        }

        return transaction(ratingsPromptSnapshot) { doc ->
            val lastAskedForReview = doc.getTimestamp("lastAskedForReview")?.toDate()?.time
            val numberOfReviewAsks = doc.getLong("numberOfReviewAsks")?.toInt() ?: 0

            if (lastAskedForReview != null) {
                onboardingRepository.updateRatingsPrompt(lastAskedForReview, numberOfReviewAsks)
            }
        }
    }

    override suspend fun importantAppsTransactionList(): List<suspend () -> Result<Unit>> {
        val documents = safeFirestoreCall {
            userDoc.collection("important_apps")
                .get()
                .await()
                .documents
        }

        return documents.mapNotNull { doc ->
            val app = doc.toObject(UserAppModel::class.java)
            if (app == null)
                return@mapNotNull null
            return@mapNotNull suspend { transaction(doc, { _ ->
                appsRepository.addImportantApp(app)
            }) }
        }
    }

    override suspend fun appSettingsTransactionsList(): List<suspend () -> Result<Unit>> {
        val documents = safeFirestoreCall {
            userDoc.collection("app_settings")
                .get()
                .await()
                .documents
        }

        return documents.map { documentSnapshot ->
            return@map suspend { transaction(documentSnapshot, { doc ->
                val packageName = doc.getString("packageName") ?: return@transaction
                val announcerVoice = doc.getString("announcerVoice")
                val notificationSources = (doc.get("notificationSources") as? List<*>)?.mapNotNull { item ->
                    when (item) {
                        is String -> NotificationSource(item)
                        is Map<*, *> -> {
                            val value = item["value"] as? String
                            val name = item["name"] as? String
                            if (value != null) NotificationSource(value, name) else null
                        }
                        else -> null
                    }
                } ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val additionalSettings = doc.get("additionalSettings") as? Map<String, String> ?: emptyMap()


                settingsRepository.saveAppSettings(
                    AppSettingsModel(
                        id = null,
                        packageName,
                        announcerVoice,
                        notificationSources,
                        additionalSettings,
                    )
                )
            }) }
        }
    }

    override suspend fun recentMessengerContactsTransactionList(): List<suspend () -> Result<Unit>> {
        val documents = safeFirestoreCall {
            userDoc.collection("recent_messenger_contacts")
                .get()
                .await()
                .documents
        }

        return documents.map { documentSnapshot ->
            return@map suspend { transaction(documentSnapshot, { doc ->
                val name = doc.getString("name")
                val lastSeen = doc.getLong("lastSeen")

                if (name != null && lastSeen != null) {
                    messengerContactsRepository.insertContact(
                        RecentMessengerContactModel(name, lastSeen)
                    )
                }
            }) }
        }
    }

    private suspend fun transaction(
        document: DocumentSnapshot, 
        onDownloadData: suspend (DocumentSnapshot) -> Unit
    ) : Result<Unit> {
        return try {
            onDownloadData(document)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
