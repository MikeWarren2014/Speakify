package com.mikewarren.speakify.data.models.scheduling

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Serializable
data class SchedulingModel(
    val statusModel: StatusModel = StatusModel.On,
    val weeklySchedule: Map<DayOfWeek, DayScheduleModel> = DayOfWeek.entries
        .associate { it to DayScheduleModel(it.getDisplayName(TextStyle.FULL,
            Locale.getDefault()))
        }
)
