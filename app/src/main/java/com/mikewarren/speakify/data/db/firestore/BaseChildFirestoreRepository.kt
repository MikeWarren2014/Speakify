package com.mikewarren.speakify.data.db.firestore

import kotlinx.coroutines.tasks.await

abstract class BaseChildFirestoreRepository: BaseMultipleFirestoreTransactionsRepository() {

    suspend fun doAllFirestoreTransactions(): Result<Unit> {
        // Ensure network is enabled before starting a batch of transactions
        try { firestore.enableNetwork().await() } catch (_: Exception) {}
        return doFirestoreTransactions(allFirestoreTransactions())
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
