package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.models.BrokenNotificationReportModel
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrokenNotificationReportRepository @Inject constructor() : BaseFirestoreRepository() {

    suspend fun submitReport(report: BrokenNotificationReportModel): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
        val reportWithAuth = report.copy(
            userId = currentUser?.uid
        )

        // Encode using kotlinx.serialization so TimestampSerializer is invoked
        val jsonElement = Json.encodeToJsonElement(BrokenNotificationReportModel.serializer(), reportWithAuth)
        val mapData = Json.decodeFromJsonElement(
            kotlinx.serialization.serializer<Map<String, Any?>>(),
            jsonElement
        )

        val documentRef = firestore
            .collection("broken_notification_reports")
            .document("${report.packageName} ${mapData["timestamp"]}")

        return writeTransaction(documentRef, mapData)
    }
}
