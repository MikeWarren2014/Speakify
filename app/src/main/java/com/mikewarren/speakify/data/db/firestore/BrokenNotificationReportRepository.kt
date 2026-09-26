package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.models.BrokenNotificationReportModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrokenNotificationReportRepository @Inject constructor() : BaseFirestoreRepository() {

    suspend fun submitReport(report: BrokenNotificationReportModel): Result<Unit> {
        val documentRef = firestore
            .collection("broken_notification_reports")
            .document()

        val currentUser = firebaseAuth.currentUser
        val reportWithAuth = report.copy(
            userId = currentUser?.uid
        )

        return writeTransaction(documentRef, reportWithAuth)
    }
}
