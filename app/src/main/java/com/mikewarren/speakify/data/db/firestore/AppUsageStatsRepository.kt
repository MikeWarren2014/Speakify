package com.mikewarren.speakify.data.db.firestore

import com.google.firebase.firestore.FieldValue
import com.mikewarren.speakify.data.db.UserAppModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageStatsRepository @Inject constructor() : BaseFirestoreRepository() {

    private val statsCollection get() = firestore.collection("app_usage_stats")

    suspend fun incrementAppCount(app: UserAppModel) {
        val docId = app.packageName.replace("/", "|")
        val docRef = statsCollection.document(docId)

        try {
            safeFirestoreCall {
                docRef.set(
                    mapOf(
                        "packageName" to app.packageName,
                        "appName" to app.appName,
                        "count" to FieldValue.increment(1)
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    suspend fun decrementAppCount(packageName: String) {
        val docId = packageName.replace("/", "|")
        val docRef = statsCollection.document(docId)

        try {
            safeFirestoreCall {
                docRef.update("count", FieldValue.increment(-1)).await()
            }
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}
