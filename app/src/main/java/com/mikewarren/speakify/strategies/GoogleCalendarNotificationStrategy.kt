package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.TimeUtils
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

class GoogleCalendarNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager
) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {

    override fun shouldSpeakify(): Boolean {
        // First, basic checks from the base class
        if (!super.shouldSpeakify()) return false

        // Log all extras to help with development
        notification.notification.extras.keySet().forEach { key ->
            Log.d("CalendarStrategy", "Extra: $key = ${notification.notification.extras.get(key)}")
        }

        // We only care about calendar events, not other notifications it might post
        // (You might need to refine this by inspecting the logged extras)
        val title = notification.notification.extras.getString(Notification.EXTRA_TITLE)
        return !title.isNullOrEmpty()
    }

    override fun textToSpeakify(): String {
        val extras = notification.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        return context.getString(R.string.notification_calendar_upcoming,
            title,
            extractRelativeTime(text) ?: text)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun extractRelativeTime(text: String): String? {
        return TimeUtils.ExtractRelativeTime(text,
            onGetDateTime = this::parseNotificationText)
    }

    fun parseNotificationText(notificationText: String): LocalDateTime? {
        // 1. Clean invisible BiDi markers, replace Narrow No-Break Spaces and Thin Spaces
        val cleaned = notificationText
            .replace("[\u200e\u200f\u202a-\u202e\u2009]".toRegex(), " ")
            .replace('\u202f', ' ') // Handles the space before AM/PM
            .trim()
            .replace("\\s+".toRegex(), " ")

        // 2. Split by any dash variation (Hyphen - or En Dash –)
        val parts = cleaned.split('–', '-')
        var timePart = parts.first().trim()

        // If the first part doesn't have AM/PM but the second part does, append it
        if (!timePart.contains("AM", ignoreCase = true) && !timePart.contains("PM", ignoreCase = true)) {
            val fullTextLower = cleaned.lowercase()
            if (fullTextLower.contains("am")) {
                timePart += " AM"
            } else if (fullTextLower.contains("pm")) {
                timePart += " PM"
            }
        }

        // 3. Try parsing with multiple patterns
        val patterns = listOf("MMM d, h:mm a",
            "h:mm a",
            "h a")
        val locales = listOf(Locale.getDefault(), Locale.US)
        val now = LocalDateTime.now()

        return patterns.firstNotNullOfOrNull { pattern ->
            locales.firstNotNullOfOrNull { locale ->
                try {
                    val formatter = DateTimeFormatterBuilder()
                        .appendPattern(pattern)
                        .parseDefaulting(ChronoField.YEAR, now.year.toLong())
                        .parseDefaulting(ChronoField.MONTH_OF_YEAR, now.monthValue.toLong())
                        .parseDefaulting(ChronoField.DAY_OF_MONTH, now.dayOfMonth.toLong())
                        .toFormatter(locale)
                    LocalDateTime.parse(timePart, formatter)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
