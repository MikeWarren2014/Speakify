package com.mikewarren.speakify.data.db.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

abstract class BaseChildFirestoreRepository: ITaggable {

    protected val firestore = FirebaseFirestore.getInstance()
    protected val firebaseAuth = FirebaseAuth.getInstance()

    protected val userDoc: DocumentReference
        get() = firestore.collection("users")
            .document(userId)

    protected val userId: String
        get() = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    abstract fun getSuccessLogMessage() : String
    abstract fun getFailureLogMessage() : String

    /**
     * Executes a Firestore call with retries if the client is offline, unavailable, 
     * or if permissions are temporarily denied (often due to auth propagation delay).
     */
    protected suspend fun <T> safeFirestoreCall(call: suspend () -> T): T {
        var retries = 5
        while (true) {
            try {
                return call()
            } catch (e: Exception) {
                val firestoreEx = (e as? FirebaseFirestoreException) ?: (e.cause as? FirebaseFirestoreException)
                val code = firestoreEx?.code
                val message = e.message ?: ""
                
                val isOffline = message.contains("offline", ignoreCase = true)
                val isPermissionDenied = code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                val isUnavailable = code == FirebaseFirestoreException.Code.UNAVAILABLE
                val isAuthMissing = e is IllegalStateException && message.contains("User not logged in")
                
                val isRetryable = isPermissionDenied || isUnavailable || isOffline || isAuthMissing
                
                if (isRetryable && retries > 0) {
                    retries--
                    Log.w(TAG, "Firestore call failed (code: $code, isAuthMissing: $isAuthMissing, isOffline: $isOffline), retrying in 2s... ($retries left)", e)
                    
                    if (isUnavailable || isOffline) {
                        try { firestore.enableNetwork().await() } catch (_: Exception) {}
                    }
                    
                    delay(2000)
                    continue
                }
                throw e
            }
        }
    }

    suspend fun doAllFirestoreTransactions(): Result<Unit> {
        // Ensure network is enabled before starting a batch of transactions
        try { firestore.enableNetwork().await() } catch (_: Exception) {}
        return doFirestoreTransactions(allFirestoreTransactions())
    }

    suspend fun doFirestoreTransactions(listOfFirebaseTransactions: List<suspend () -> Result<Unit>>) : Result<Unit> {
        var currentResult: Result<Unit> = Result.success(Unit)

        try {
            val failedTransaction = listOfFirebaseTransactions
                .firstOrNull {
                    currentResult = try {
                        it()
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                    currentResult.isFailure
                }

            if (failedTransaction != null) {
                Log.e(TAG, getFailureLogMessage(), currentResult.exceptionOrNull())
                return currentResult
            }

            Log.d(TAG, getSuccessLogMessage())
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, getFailureLogMessage(), e)
            return Result.failure(e)
        }
    }

    open suspend fun allFirestoreTransactions(): List<suspend () -> Result<Unit>> {
        return listOf(
            this::settingsTransaction,
            { doFirestoreTransactions(importantAppsTransactionList()) },
            { doFirestoreTransactions(appSettingsTransactionsList()) },
            { doFirestoreTransactions(recentMessengerContactsTransactionList()) },
        )
    }

    abstract suspend fun settingsTransaction() : Result<Unit>
    abstract suspend fun importantAppsTransactionList() : List<suspend () -> Result<Unit>>
    abstract suspend fun appSettingsTransactionsList() : List<suspend () -> Result<Unit>>

    abstract suspend fun recentMessengerContactsTransactionList(): List<suspend () -> Result<Unit>>
}
