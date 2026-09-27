package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.NotificationAuditLogModel
import kotlinx.coroutines.flow.Flow

interface NotificationAuditRepository {
    fun getRecentLogs(windowMillis: Long = 30 * Constants.OneMinute): Flow<List<NotificationAuditLogModel>>
    suspend fun getRecentLogsList(windowMillis: Long = 30 * Constants.OneMinute): List<NotificationAuditLogModel>
    suspend fun log(logModel: NotificationAuditLogModel)
    suspend fun clearOldLogs(retentionMillis: Long = Constants.OneHour)
}
