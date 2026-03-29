package com.mikewarren.speakify.data.models.scheduling

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.serialization.Serializable

@Serializable
enum class DayScheduleType(val labelUiText: UiText) {
    ALL_DAY(UiText.StringResource(R.string.day_schedule_all_day)),
    SELECT_TIMES(UiText.StringResource(R.string.day_schedule_select_times)),
    OFF(UiText.StringResource(R.string.day_schedule_off))
}

@Serializable
data class DayScheduleModel(
    val dayName: String,
    val type: DayScheduleType = DayScheduleType.ALL_DAY,
    val fromTime: String = "09:00",
    val toTime: String = "17:00"
)
