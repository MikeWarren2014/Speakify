package com.mikewarren.speakify.data.db.firestore

import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDeletionFirestoreRepository @Inject constructor() : BaseChildFirestoreRepository() {

    override fun getSuccessLogMessage(): String = "User data deleted successfully for user $userId"

    override fun getFailureLogMessage(): String = "Failed to delete user data"

    suspend fun deleteUserData(): Result<Unit> {
        return try {
            // Delete sub-collections first if necessary, 
            // but for simplicity we delete the document.
            // Note: Firestore doesn't automatically delete sub-collections when a document is deleted.
            
            val collections = listOf("config", "important_apps", "app_settings", "recent_messenger_contacts")
            collections.forEach { collectionName ->
                val snapshots = safeFirestoreCall {
                    userDoc.collection(collectionName).get().await()
                }
                snapshots.documents.forEach { doc ->
                    safeFirestoreCall {
                        doc.reference.delete().await()
                    }
                }
            }
            
            safeFirestoreCall {
                userDoc.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun settingsTransaction(): Result<Unit> = Result.success(Unit)
    override suspend fun importantAppsTransactionList(): List<suspend () -> Result<Unit>> = emptyList()
    override suspend fun appSettingsTransactionsList(): List<suspend () -> Result<Unit>> = emptyList()
    override suspend fun recentMessengerContactsTransactionList(): List<suspend () -> Result<Unit>> = emptyList()
}
