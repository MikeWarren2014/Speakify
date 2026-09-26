package com.mikewarren.speakify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationAuditLogDao {
    @Query("SELECT * FROM notification_audit_log WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecentLogs(sinceTimestamp: Long): Flow<List<NotificationAuditLogModel>>

    @Query("SELECT * FROM notification_audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsList(limit: Int = 50): Flow<List<NotificationAuditLogModel>>

    @Query("SELECT * FROM notification_audit_log WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentLogsSince(sinceTimestamp: Long): List<NotificationAuditLogModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationAuditLogModel): Long

    @Query("DELETE FROM notification_audit_log WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long)

    @Query("DELETE FROM notification_audit_log WHERE id NOT IN (SELECT id FROM notification_audit_log ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun trimLogs(keepCount: Int = 50)

    @Query("DELETE FROM notification_audit_log")
    suspend fun clearAll()
}
