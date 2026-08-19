package com.mikewarren.speakify.utils

import android.content.Context
import android.text.format.DateUtils
import com.mikewarren.speakify.R
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

object TimeUtils {
    fun GetLocalDateTimeFrom(dayOfWeek: DayOfWeek, hhMM: String): LocalDateTime {
        val now = LocalDateTime.now()
        var localDateTime = now.with(dayOfWeek)

        // If the day has already passed this week, move to next week
        if (localDateTime.isBefore(now.withHour(0).withMinute(0))) {
            localDateTime = localDateTime.plusWeeks(1)
        }

        return GetLocalDateTimeWithHHMM(localDateTime, hhMM)
    }

    fun GetLocalDateTimeWithHHMM(localDateTime: LocalDateTime = LocalDateTime.now(), hhMM: String): LocalDateTime {
        return try {
            // Handle common formats like "2:30 PM" or "14:30"
            val formatter = DateTimeFormatter.ofPattern("[h:mm a][H:mm]", Locale.US)
            val time = LocalTime.parse(hhMM.uppercase(), formatter)
            localDateTime.with(time)
        } catch (e: Exception) {
            // Fallback for the original fragile logic if needed, but improved
            val parts = hhMM.split(" ")
            val timeParts = parts[0].split(":")
            var hours = timeParts[0].toInt()
            val minutes = timeParts[1].toInt()
            
            if (parts.size > 1) {
                val amPM = parts[1].lowercase()
                if (amPM == "pm" && hours < 12) hours += 12
                if (amPM == "am" && hours == 12) hours = 0
            }
            localDateTime.withHour(hours).withMinute(minutes)
        }
    }

    fun ExtractRelativeTime(context: Context, text: String, onGetDateTime: (String) -> LocalDateTime?): String? {
        val localDateTime = onGetDateTime(text) ?: return null

        return try {
            val rawRelativeTimeSpanString = DateUtils.getRelativeTimeSpanString(

                localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()

            val tokens = rawRelativeTimeSpanString.split(" ")
            if (tokens.contains("0"))
                // return the string for "Right now"
                return context.getString(R.string.relative_time_right_now)

            return rawRelativeTimeSpanString
        } catch (e: Exception) {
            null
        }
    }

    fun GetQuantityString(context: Context, localDateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(now, localDateTime)
        val absMinutes = Math.abs(duration.toMinutes())

        val oneHour = 60
        val oneDay = 24 * oneHour

        return when {
            absMinutes < oneHour -> {
                context.resources.getQuantityString(R.plurals.minutes_quantity, absMinutes.toInt(), absMinutes)
            }
            absMinutes < oneDay -> {
                val hours = absMinutes / oneHour
                context.resources.getQuantityString(R.plurals.hours_quantity, hours.toInt(), hours)
            }
            else -> {
                val days = absMinutes / oneDay
                context.resources.getQuantityString(R.plurals.days_quantity, days.toInt(), days)
            }
        }
    }

    fun ExtractTimeRange(text: String): Array<String> {
        // Matches HH:MM or H:MM, optionally followed by AM/PM (case insensitive)
        val timeRegex = """(\d{1,2}:\d{2}(?:\s?[AaPp][Mm])?)""".toRegex()

        val matches = timeRegex.findAll(text).map { it.value }.toList()
        if (matches.size >= 2) {
            return arrayOf(matches[0], matches[1])
        }

        return emptyArray()
    }

    fun GetLocalDateTimeFromTimeString(timeString: String): LocalDateTime? {
        val ampmString = ToAMPMString(timeString)

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
                    LocalDateTime.parse(ampmString, formatter)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    fun ToAMPMString(timeString: String): String {
        if (':' !in timeString)
            throw IllegalArgumentException("timePart must contain a colon")

        if (SearchUtils.HasAnyMatches(listOf("am", "pm"), timeString.lowercase()))
            return timeString

        val currentHour = LocalDateTime.now()
            .hour
        val extractedHour = timeString.split(":")[0]
                .toInt()

        return if (extractedHour >= currentHour)
            "$timeString AM"
        else
            "$timeString PM"
    }
}
