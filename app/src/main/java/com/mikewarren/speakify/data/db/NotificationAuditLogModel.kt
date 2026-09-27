package com.mikewarren.speakify.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notification_audit_log")
data class NotificationAuditLogModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_display_name")
    val appDisplayName: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "raw_title")
    val rawTitle: String? = null,

    @ColumnInfo(name = "raw_text")
    val rawText: String? = null,

    @ColumnInfo(name = "speakified_text")
    val speakifiedText: String? = null,

    @ColumnInfo(name = "silence_reason")
    val silenceReason: String? = null,

    @ColumnInfo(name = "notification_key")
    val notificationKey: String? = null
) {
    companion object {
        const val SilenceReasonGatekeeperMuted = "GATEKEEPER_MUTED"
        const val SilenceReasonHandledByPhoneReceiver = "HANDLED_BY_PHONE_RECEIVER"
        const val SilenceReasonDebounced = "DEBOUNCED"
        const val SilenceReasonStrategyFiltered = "STRATEGY_FILTERED"
        const val SilenceReasonUnknown = "SILENCED_UNKNOWN"
        fun From(packageName: String,
                 appDisplayName: String,
                 rawTitle: String?,
                 rawText: String?,
                 speakifiedText: String?,
                 silenceReason: String?,
                 notificationKey: String?): NotificationAuditLogModel {
            val silenceReasonToUse = silenceReason ?: if (speakifiedText != null) null else this.SilenceReasonUnknown

            return NotificationAuditLogModel(
                packageName = packageName,
                appDisplayName = appDisplayName,
                rawTitle = rawTitle,
                rawText = rawText,
                speakifiedText = speakifiedText,
                silenceReason = silenceReasonToUse,
                notificationKey = notificationKey,
            )
        }
    }
}