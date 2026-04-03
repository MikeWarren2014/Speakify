package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import android.text.format.DateUtils
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.TimeUtils
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale

class GeohNotificationStrategy(notification: StatusBarNotification,
                               appSettingsModel: AppSettingsModel?,
                               context: Context,
                               ttsManager: TTSManager) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    val title = NotificationExtractionUtils.ExtractTitle(notification)
    val text = NotificationExtractionUtils.ExtractText(notification)
    
    enum class NotificationType {
        UpcomingShift,
        MissedClockIn,
        MissedClockOut,
        Other,
    }
    
    fun getNotificationType(): NotificationType {
        val importantExtrasList = listOf(title, text)

        if (SearchUtils.HasAnyMatches(importantExtrasList, context.getString(R.string.geoh_upcoming_keyword)))
            return NotificationType.UpcomingShift

        if (SearchUtils.HasAnyMatches(importantExtrasList, context.getString(R.string.geoh_missed_clock_in_keyword)))
            return NotificationType.MissedClockIn

        if (SearchUtils.HasAnyMatches(importantExtrasList, context.getString(R.string.geoh_missed_clock_out_keyword)))
            return NotificationType.MissedClockOut
        
        return NotificationType.Other
    }

    override fun textToSpeakify(): String {
        val notificationType = getNotificationType()
        if (notificationType == NotificationType.UpcomingShift) {
            val relativeTime = extractRelativeTime(text)
            if (relativeTime != null) {
                return context.getString(R.string.geoh_strategy_upcoming_shift, relativeTime)
            }
        }
        return text
    }

    private fun extractRelativeTime(text: String): String? {
        // Matches "on Wednesday, 2:30 PM" or "on Wednesday, 14:30"
        val regex = """on\s+(?<dayOfWeek>\w+),\s+(?<date>\w+\s+\d{1,2}\s+\d{4},)\s+(?<time>\d{1,2}:\d{2}(?:\s?[APMapm]{2})?)""".toRegex()
        val matchResult = regex.find(text) ?: return null

        val dayString = matchResult.groups["dayOfWeek"]?.value ?: return null
        val timeString = matchResult.groups["time"]?.value ?: return null

        return try {
            val dayOfWeek = DayOfWeek.valueOf(dayString.uppercase(Locale.US))
            val localDateTime = TimeUtils.GetLocalDateTimeFrom(dayOfWeek, timeString)
            val millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (e: Exception) {
            null
        }
    }

    override fun shouldSpeakify(): Boolean {
        return getNotificationType() != NotificationType.Other
    }
}
