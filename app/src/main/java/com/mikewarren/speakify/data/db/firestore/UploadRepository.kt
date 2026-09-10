package com.mikewarren.speakify.data.db.firestore

import android.util.Log
import com.clerk.api.Clerk
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.models.FeedbackModel
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val schedulingRepository: SchedulingRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val appUsageStatsRepository: AppUsageStatsRepository
): BaseChildFirestoreRepository() {


    override fun getSuccessLogMessage(): String {
        return "All data uploaded successfully for user ${userId}"
    }

    override fun getFailureLogMessage(): String {
        return "Failed to upload data"
    }

    override suspend fun allFirestoreTransactions(): List<suspend () -> Result<Unit>> {
        if (isAnonymous) {
            // Stats Only mode for anonymous users
            return listOf { doFirestoreTransactions(importantAppsTransactionList()) }
        }

        return listOf(this::writeClerkUserData) +
            super.allFirestoreTransactions()
    }

    override suspend fun settingsTransaction(): Result<Unit> {
        return writeTransaction(userDoc.collection("config")
            .document("settings"),
            hashMapOf(
                "useDarkTheme" to settingsRepository.useDarkTheme.first(),
                "selectedTTSVoice" to settingsRepository.selectedTTSVoice.first(),
                "maximizeVolumeOnScreenOff" to settingsRepository.maximizeVolumeOnScreenOff.first(),
                "minVolume" to settingsRepository.minVolume.first(),
                "isCrashlyticsEnabled" to settingsRepository.isCrashlyticsEnabled.first()
            ))
    }

    override suspend fun schedulingTransaction(): Result<Unit> {
        val model = schedulingRepository.scheduling.first()
        val statusData = hashMapOf(
            "isAppOn" to model.statusModel.isAppOn,
            "turnOnTime" to (model.statusModel as? StatusModel.Off)?.turnOnTime
        )

        val weeklyScheduleData = model.weeklySchedule.entries.associate { (day, schedule) ->
            day.name to hashMapOf(
                "dayName" to schedule.dayName,
                "type" to schedule.type.name,
                "fromTime" to schedule.fromTime,
                "toTime" to schedule.toTime
            )
        }

        val data = hashMapOf(
            "status" to statusData,
            "weeklySchedule" to weeklyScheduleData
        )

        return writeTransaction(userDoc.collection("config")
            .document("schedule"),
            data
        )
    }

    override suspend fun onboardingTransaction(): Result<Unit> {
        val model = onboardingRepository.onboardingModel.first()
        val onboardingData = hashMapOf(
            "appOpenCount" to model.appOpenCount,
            "speakificationCount" to model.speakificationCount,
            "onboardingStep" to model.onboardingStep::class.simpleName,
            "primaryGoal" to model.primaryGoal,
            "hasShownRatingsPrompt" to model.hasShownRatingsPrompt,
            "hasShownTrialConversionPrompt" to model.hasShownTrialConversionPrompt,
            "importantAppCategories" to model.importantAppCategories.map {
                hashMapOf(
                    "category" to it.category.name,
                    "isSatisfied" to it.isSatisfied
                )
            },
            "timestamp" to Timestamp.now()
        )

        return writeTransaction(userDoc.collection("onboarding")
            .document("onboarding"),
            onboardingData
        )
    }

    override suspend fun feedbackTransaction(): Result<Unit> {
        val model = onboardingRepository.onboardingModel.first()
        val feedback = model.feedback

        feedback?.let {
            return writeTransaction(userDoc.collection("onboarding")
                .document("feedback"),
                it
            )
        }

        return Result.success(Unit)
    }

    override suspend fun ratingsPromptTransaction(): Result<Unit> {
        val model = onboardingRepository.onboardingModel.first()
        val ratingsPrompt = model.ratingsPrompt

        val data = hashMapOf(
            "lastAskedForReview" to ratingsPrompt.lastAskedForReview?.let { Timestamp(Date(it)) },
            "numberOfReviewAsks" to ratingsPrompt.numberOfReviewAsks
        )

        return writeTransaction(userDoc.collection("onboarding")
            .document("ratingsPrompt"),
            data
        )
    }

    override suspend fun appSettingsTransactionsList(): List<suspend () -> Result<Unit>> {
        val appSettingsCollection = userDoc.collection("app_settings")
        val appSettingsMap = settingsRepository.appSettings.first()

        val clearStaleRecordsTask: suspend () -> Result<Unit> = {
            clearStaleRecordsTransaction(
                appSettingsCollection,
                { documentSnapshot, dataMap ->
                    val packageName = documentSnapshot.id
                    !dataMap.containsKey(packageName)
                },
                appSettingsMap
            )
        }

        // We might want to clear old apps or just merge.
        // For now, we merge/update existing ones.
        return appSettingsMap
            .filter { it.key.isNotBlank() }
            .map { (packageName, model) ->
            val docId = packageName.replace("/", "|")
            // Firestore doesn't like custom classes with Long? id if not configured,
            // so we map it to a hashmap for safety.
            val appData = hashMapOf(
                "packageName" to model.packageName,
                "announcerVoice" to model.announcerVoice,
                "notificationSources" to model.notificationSources,
                "additionalSettings" to model.additionalSettings,
            )
            return@map suspend { writeTransaction(appSettingsCollection.document(docId), appData) }
        } + listOf(clearStaleRecordsTask)

    }

    override suspend fun importantAppsTransactionList(): List<suspend () -> Result<Unit>> {
        val importantAppsCollection = userDoc.collection("important_apps")
        val importantAppsList = appsRepository.importantApps.first()
            .filter { it.packageName.isNotBlank() }

        // Fetch existing docs once to determine additions vs updates
        val existingDocs = try {
            safeFirestoreCall {
                importantAppsCollection.get().await().documents
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch existing important apps", e)
            emptyList()
        }
        val existingDocIds = existingDocs.map { it.id }.toSet()

        val clearStaleRecordsTask: suspend () -> Result<Unit> = {
            clearStaleRecordsTransaction(
                importantAppsCollection,
                { documentSnapshot, modelList ->
                    val docPackageName = documentSnapshot.id
                    modelList.none { it.packageName.replace("/", "|") == docPackageName }
                },
                importantAppsList,
                true, // Is important apps collection
                existingDocs // Reuse fetched docs
            )
        }

        val uploadTasks = importantAppsList.map { app ->
            val docId = app.packageName.replace("/", "|")
            val isNew = !existingDocIds.contains(docId)
            suspend { writeImportantAppTransaction(importantAppsCollection, app, isNew) }
        }

        return listOf(clearStaleRecordsTask) + uploadTasks
    }

    private suspend fun writeImportantAppTransaction(
        collection: CollectionReference,
        app: com.mikewarren.speakify.data.db.UserAppModel,
        isNew: Boolean
    ): Result<Unit> {
        val docId = app.packageName.replace("/", "|")
        val docRef = collection.document(docId)

        return try {
            if (isNew) {
                appUsageStatsRepository.incrementAppCount(app)
            }
            if (isAnonymous) {
                return Result.success(Unit)
            }
            writeTransaction(docRef, app)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recentMessengerContactsTransactionList(): List<suspend () -> Result<Unit>> {
        val recentMessengerContactsCollection = userDoc.collection("recent_messenger_contacts")
        val recentMessengerContactsList = messengerContactsRepository.recentContacts.first()
            .filter { it.name.isNotBlank() }

        val clearStaleRecordsTask: suspend () -> Result<Unit> = {
            clearStaleRecordsTransaction(
                recentMessengerContactsCollection,
                { documentSnapshot, modelList ->
                    val docId = documentSnapshot.id
                    modelList.none { it.name.replace("/", "|") == docId }
                },
                recentMessengerContactsList,
                false // Not important apps
            )
        }

        val uploadTasks = recentMessengerContactsList.map { contact ->
            val docId = contact.name.replace("/", "|")
            suspend { writeTransaction(recentMessengerContactsCollection.document(docId), contact) }
        }

        return listOf(clearStaleRecordsTask) + uploadTasks
    }

    private suspend fun <T> clearStaleRecordsTransaction(
        documentCollection: CollectionReference,
        onCheckStaleRecord: suspend (DocumentSnapshot, T) -> Boolean,
        data: T,
        isImportantApps: Boolean = false,
        preFetchedDocuments: List<DocumentSnapshot>? = null
    ) : Result<Unit> {
        return try {
            val documents = preFetchedDocuments ?: safeFirestoreCall {
                documentCollection.get()
                    .await()
                    .documents
            }

            documents
                .filter { documentSnapshot ->
                    onCheckStaleRecord(documentSnapshot, data)
                }
                .forEach { documentSnapshot ->
                    if (isImportantApps) {
                        val packageName = documentSnapshot.id.replace("|", "/")
                        appUsageStatsRepository.decrementAppCount(packageName)
                    }
                    safeFirestoreCall {
                        documentSnapshot.reference.delete()
                            .await()
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
