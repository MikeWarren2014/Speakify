package com.mikewarren.speakify.data.db

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val notificationKey: String? = null,

    @ColumnInfo(name = "actions")
    val actions: String? = null,

    @ColumnInfo(name = "extras")
    val extras: String? = null
) {
    companion object {
        const val SilenceReasonGatekeeperMuted = "GATEKEEPER_MUTED"
        const val SilenceReasonHandledByPhoneReceiver = "HANDLED_BY_PHONE_RECEIVER"
        const val SilenceReasonDebounced = "DEBOUNCED"
        const val SilenceReasonStrategyFiltered = "STRATEGY_FILTERED"
        const val SilenceReasonUnknown = "SILENCED_UNKNOWN"

        fun From(
            packageName: String,
            appDisplayName: String,
            rawTitle: String?,
            rawText: String?,
            speakifiedText: String?,
            silenceReason: String?,
            notificationKey: String?,
            actions: String? = null,
            extras: String? = null
        ): NotificationAuditLogModel {
            val silenceReasonToUse = silenceReason ?: if (speakifiedText != null) null else SilenceReasonUnknown

            return NotificationAuditLogModel(
                packageName = packageName,
                appDisplayName = appDisplayName,
                rawTitle = rawTitle,
                rawText = rawText,
                speakifiedText = speakifiedText,
                silenceReason = silenceReasonToUse,
                notificationKey = notificationKey,
                actions = actions,
                extras = extras
            )
        }

        fun From(
            sbn: StatusBarNotification,
            appDisplayName: String,
            rawTitle: String?,
            rawText: String?,
            speakifiedText: String? = null,
            silenceReason: String? = null
        ): NotificationAuditLogModel {
            return From(
                packageName = sbn.packageName,
                appDisplayName = appDisplayName,
                rawTitle = rawTitle,
                rawText = rawText,
                speakifiedText = speakifiedText,
                silenceReason = silenceReason,
                notificationKey = sbn.key,
                actions = extractActionTitles(sbn.notification.actions),
                extras = extractSafeExtras(sbn.notification.extras)
            )
        }

        fun extractActionTitles(actions: Array<Notification.Action>?): String? {
            if (actions.isNullOrEmpty()) return null
            val titles = actions.mapNotNull { it.title?.toString()?.trim() }.filter { it.isNotBlank() }
            return if (titles.isNotEmpty()) titles.joinToString(", ") else null
        }

        fun extractSafeExtras(bundle: Bundle?): String? {
            if (bundle == null) return null
            val targetKeys = listOf(
                Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_SUMMARY_TEXT,
                Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_INFO_TEXT,
                Notification.EXTRA_CONVERSATION_TITLE,
                Notification.EXTRA_TEMPLATE,
                Notification.EXTRA_SHOW_CHRONOMETER,
                Notification.EXTRA_SHOW_WHEN
            )
            val map = mutableMapOf<String, String>()
            for (key in targetKeys) {
                try {
                    @Suppress("DEPRECATION")
                    val value = bundle.get(key)
                    if (value is CharSequence || value is Number || value is Boolean) {
                        map[key] = value.toString()
                    }
                } catch (_: Exception) {
                    // Ignore unparceling errors
                }
            }
            return if (map.isNotEmpty()) Json.encodeToString(map) else null
        }
    }
}
