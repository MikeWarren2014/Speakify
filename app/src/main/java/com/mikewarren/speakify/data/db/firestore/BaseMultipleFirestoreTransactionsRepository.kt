package com.mikewarren.speakify.data.db.firestore

import android.util.Log
import com.mikewarren.speakify.utils.log.IResultLoggable

abstract class BaseMultipleFirestoreTransactionsRepository : BaseUserFirestoreRepository(), IResultLoggable {
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
}