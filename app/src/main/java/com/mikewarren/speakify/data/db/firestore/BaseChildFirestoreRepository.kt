package com.mikewarren.speakify.data.db.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.mikewarren.speakify.utils.log.ITaggable

abstract class BaseChildFirestoreRepository: ITaggable {

    private val firestore = FirebaseFirestore.getInstance()
    protected val firebaseAuth = FirebaseAuth.getInstance()

    protected val userDoc: DocumentReference
        get() = firestore.collection("users")
            .document(userId)

    protected val userId: String
        get() = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    abstract fun getSuccessLogMessage() : String
    abstract fun getFailureLogMessage() : String

    suspend fun doAllFirestoreTransactions(): Result<Unit> {
        return doFirestoreTransactions(allFirestoreTransactions())
    }

    suspend fun doFirestoreTransactions(listOfFirebaseTransactions: List<suspend () -> Result<Unit>>) : Result<Unit> {
        var currentResult: Result<Unit> = Result.success(Unit)

        val failedTransaction = listOfFirebaseTransactions
            .firstOrNull {
                currentResult = it()
                currentResult.isFailure
            }
        if (failedTransaction != null) {
            Log.e(TAG, getFailureLogMessage(), currentResult.exceptionOrNull())
            return currentResult
        }

        Log.d(TAG, getSuccessLogMessage())
        return Result.success(Unit)
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