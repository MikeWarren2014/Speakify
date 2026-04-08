package com.mikewarren.speakify.data.db.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import com.mikewarren.speakify.data.db.UserAppModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,
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
            return Result.failure(IllegalStateException("Settings document does not exist"))
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
                @Suppress("UNCHECKED_CAST")
                val notificationSources = doc.get("notificationSources") as? List<String> ?: emptyList()
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
