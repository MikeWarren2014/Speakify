package com.mikewarren.speakify.data.models

enum class DayScheduleType(val label: String) {
    ALL_DAY("All Day"),
    SELECT_TIMES("Select Times"),
    OFF("Off")
}

data class DayScheduleState(
    val dayName: String,
    val type: DayScheduleType = DayScheduleType.ALL_DAY,
    val fromTime: String = "09:00",
    val toTime: String = "17:00"
)