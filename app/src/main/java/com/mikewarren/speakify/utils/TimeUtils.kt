package com.mikewarren.speakify.utils

import java.time.DayOfWeek
import java.time.LocalDateTime

object TimeUtils {
    fun GetLocalDateTimeFrom(dayOfWeek: DayOfWeek, hhMM: String): LocalDateTime {
        val now = LocalDateTime.now()

        var localDateTime = LocalDateTime.now()
            .with(dayOfWeek)

        if (dayOfWeek < now.dayOfWeek) {
            localDateTime = localDateTime.plusWeeks(1)
        }

        return GetLocalDateTimeWithHHMM(localDateTime, hhMM)
    }

    fun GetLocalDateTimeWithHHMM(localDateTime: LocalDateTime = LocalDateTime.now(), hhMM: String): LocalDateTime {
        val amPM = hhMM.split(' ')[1]
            .lowercase()

        var hours = hhMM.substring(0, 2).toInt()
        if (amPM == "pm") {
            hours += 12
        }

        return localDateTime
            .withHour(hours)
            .withMinute(hhMM.substring(3, 5).toInt())

    }
}