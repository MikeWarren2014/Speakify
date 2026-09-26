package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.NotificationAuditLogModel
import kotlinx.coroutines.flow.Flow

interface NotificationAuditRepository {
    fun getRecentLogs(windowMillis: Long = 30 * Constants.OneMinute): Flow<List<NotificationAuditLogModel>>
    suspend fun getRecentLogsList(windowMillis: Long = 30 * Constants.OneMinute): List<NotificationAuditLogModel>
    suspend fun logEvent(
        packageName: String,
        appDisplayName: String,
        rawTitle: String?,
        rawText: String?,
        speakifiedText: String?,
        silenceReason: String?,
        notificationKey: String? = null
    )
    suspend fun clearOldLogs(retentionMillis: Long = Constants.OneHour)
}
