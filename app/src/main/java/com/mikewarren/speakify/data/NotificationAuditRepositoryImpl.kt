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

    override suspend fun log(logModel: NotificationAuditLogModel) {
        dao.insertLog(logModel)
        dao.trimLogs(keepCount = 50)
    }

    override suspend fun clearOldLogs(retentionMillis: Long) {
        val cutoff = System.currentTimeMillis() - retentionMillis
        dao.deleteOldLogs(cutoff)
    }
}
