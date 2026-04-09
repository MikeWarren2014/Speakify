package com.mikewarren.speakify.data.db.firestore

import android.util.Log
import com.mikewarren.speakify.data.BaseUserFirestoreRepository
import com.mikewarren.speakify.utils.log.IResultLoggable
import kotlinx.coroutines.tasks.await

abstract class BaseChildFirestoreRepository: BaseUserFirestoreRepository(), IResultLoggable {

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

            logSuccessResult()
            return Result.success(Unit)
        } catch (e: Exception) {
            logFailureResult(e)
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
