package com.mikewarren.speakify.data.db.firestore

import com.clerk.api.Clerk
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.models.FeedbackModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,
    private val onboardingRepository: OnboardingRepository,
): BaseChildFirestoreRepository() {


    override fun getSuccessLogMessage(): String {
        return "All data uploaded successfully for user ${userId}"
    }

    override fun getFailureLogMessage(): String {
        return "Failed to upload data"
    }

    override suspend fun allFirestoreTransactions(): List<suspend () -> Result<Unit>> {
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
            "timestamp" to com.google.firebase.Timestamp.now()
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

        val clearStaleRecordsTask: suspend () -> Result<Unit> = {
            clearStaleRecordsTransaction(
                importantAppsCollection,
                { documentSnapshot, modelList ->
                    val docPackageName = documentSnapshot.id
                    modelList.none { it.packageName.replace("/", "|") == docPackageName }
                },
                importantAppsList,
            )
        }

        val uploadTasks = importantAppsList.map { app ->
            val docId = app.packageName.replace("/", "|")
            suspend { writeTransaction(importantAppsCollection.document(docId), app) }
        }

        return listOf(clearStaleRecordsTask) + uploadTasks
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
        data: T
    ) : Result<Unit> {
        return try {
            val documents = safeFirestoreCall {
                documentCollection.get()
                    .await()
                    .documents
            }

            documents
                .filter { documentSnapshot ->
                    onCheckStaleRecord(documentSnapshot, data)
                }
                .forEach { documentSnapshot ->
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
