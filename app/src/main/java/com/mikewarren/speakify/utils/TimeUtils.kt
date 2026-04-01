package com.mikewarren.speakify.utils

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
}
