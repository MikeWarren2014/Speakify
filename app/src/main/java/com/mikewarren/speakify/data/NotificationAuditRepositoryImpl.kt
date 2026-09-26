package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.NotificationAuditLogDao
import com.mikewarren.speakify.data.db.NotificationAuditLogModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAuditRepositoryImpl @Inject constructor(
    private val dao: NotificationAuditLogDao
) : NotificationAuditRepository {

    override fun getRecentLogs(windowMillis: Long): Flow<List<NotificationAuditLogModel>> {
        val sinceTimestamp = System.currentTimeMillis() - windowMillis
        return dao.getRecentLogs(sinceTimestamp)
    }

    override suspend fun getRecentLogsList(windowMillis: Long): List<NotificationAuditLogModel> {
        val sinceTimestamp = System.currentTimeMillis() - windowMillis
        return dao.getRecentLogsSince(sinceTimestamp)
    }

    override suspend fun logEvent(
        packageName: String,
        appDisplayName: String,
        rawTitle: String?,
        rawText: String?,
        speakifiedText: String?,
        silenceReason: String?,
        notificationKey: String?
    ) {
        val log = NotificationAuditLogModel(
            packageName = packageName,
            appDisplayName = appDisplayName,
            rawTitle = rawTitle,
            rawText = rawText,
            speakifiedText = speakifiedText,
            silenceReason = silenceReason ?: if (speakifiedText != null) null else "SILENCED_UNKNOWN",
            notificationKey = notificationKey
        )
        dao.insertLog(log)
        dao.trimLogs(keepCount = 50)
    }

    override suspend fun clearOldLogs(retentionMillis: Long) {
        val cutoff = System.currentTimeMillis() - retentionMillis
        dao.deleteOldLogs(cutoff)
    }
}
