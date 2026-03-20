package com.mikewarren.speakify.data.db.firestore

import com.clerk.api.Clerk
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,
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

    suspend fun writeClerkUserData(): Result<Unit> {
        val clerkUserId = Clerk.user?.id

        if (clerkUserId == null)
            return Result.failure(IllegalStateException("Clerk user ID is null. Is user even logged in?"))

        return transaction(userDoc,
            hashMapOf(
                "clerkUserId" to clerkUserId,
                "userEmail" to Clerk.user?.emailAddresses!!.first().emailAddress,
            ))


    }

    override suspend fun settingsTransaction(): Result<Unit> {
        return transaction(userDoc.collection("config")
            .document("settings"),
            hashMapOf(
                "useDarkTheme" to settingsRepository.useDarkTheme.first(),
                "selectedTTSVoice" to settingsRepository.selectedTTSVoice.first(),
                "maximizeVolumeOnScreenOff" to settingsRepository.maximizeVolumeOnScreenOff.first(),
                "minVolume" to settingsRepository.minVolume.first(),
                "isCrashlyticsEnabled" to settingsRepository.isCrashlyticsEnabled.first()
            ))
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
        return appSettingsMap.map { (packageName, model) ->
            val docId = packageName
            // Firestore doesn't like custom classes with Long? id if not configured,
            // so we map it to a hashmap for safety.
            val appData = hashMapOf(
                "packageName" to model.packageName,
                "announcerVoice" to model.announcerVoice,
                "notificationSources" to model.notificationSources,
                "additionalSettings" to model.additionalSettings,
            )
            return@map suspend { transaction(appSettingsCollection.document(docId), appData) }
        } + listOf(clearStaleRecordsTask)

    }

    override suspend fun importantAppsTransactionList(): List<suspend () -> Result<Unit>> {
        val importantAppsCollection = userDoc.collection("important_apps")
        val importantAppsList = appsRepository.importantApps.first()

        val clearStaleRecordsTask: suspend () -> Result<Unit> = {
            clearStaleRecordsTransaction(
                importantAppsCollection,
                { documentSnapshot, modelList ->
                    val docPackageName = documentSnapshot.id
                    modelList.none { it.packageName == docPackageName }
                },
                importantAppsList,
            )
        }

        val uploadTasks = importantAppsList.map { app ->
            val docId = app.packageName
            suspend { transaction(importantAppsCollection.document(docId), app) }
        }

        return listOf(clearStaleRecordsTask) + uploadTasks
    }

    override suspend fun recentMessengerContactsTransactionList(): List<suspend () -> Result<Unit>> {
        val recentMessengerContactsCollection = userDoc.collection("recent_messenger_contacts")
        val recentMessengerContactsList = messengerContactsRepository.recentContacts.first()

        val clearStaleRecordsTask: suspend () -> Result<Unit> = {
            clearStaleRecordsTransaction(
                recentMessengerContactsCollection,
                { documentSnapshot, modelList ->
                    val contactName = documentSnapshot.id
                    modelList.none { it.name == contactName }
                },
                recentMessengerContactsList,
            )
        }

        val uploadTasks = recentMessengerContactsList.map { contact ->
            val docId = contact.name
            suspend { transaction(recentMessengerContactsCollection.document(docId), contact) }
        }

        return listOf(clearStaleRecordsTask) + uploadTasks
    }

    private suspend fun transaction(document: DocumentReference, data: Any) : Result<Unit> {
        return try {
            document
                .set(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> clearStaleRecordsTransaction(
        documentCollection: CollectionReference,
        onCheckStaleRecord: suspend (DocumentSnapshot, T) -> Boolean,
        data: T
    ) : Result<Unit> {
        return try {
            documentCollection.get()
                .await()
                .documents
                .filter { documentSnapshot ->
                    onCheckStaleRecord(documentSnapshot, data)
                }
                .forEach { documentSnapshot ->
                    documentSnapshot.reference.delete()
                        .await()
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
