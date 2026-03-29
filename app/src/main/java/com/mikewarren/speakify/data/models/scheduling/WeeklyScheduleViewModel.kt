package com.mikewarren.speakify.data.models.scheduling

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek

class WeeklyScheduleViewModel(
    val initialSchedule: Map<DayOfWeek, DayScheduleModel>,
    val onSave: (Map<DayOfWeek, DayScheduleModel>) -> Any,
): ViewModel() {
    var _weeklyScheduleFlow: MutableStateFlow<Map<DayOfWeek, DayScheduleModel>> = MutableStateFlow(initialSchedule)
    val weeklyScheduleFlow: StateFlow<Map<DayOfWeek, DayScheduleModel>> = _weeklyScheduleFlow.asStateFlow()


    fun updateDayType(dayOfWeek: DayOfWeek, newType: DayScheduleType) {
        updateDayScheduleModel(dayOfWeek, _weeklyScheduleFlow.value[dayOfWeek]
            !!.copy(type = newType))
    }

    fun updateDayFromTime(dayOfWeek: DayOfWeek, newFromTime: String) {
        updateDayScheduleModel(dayOfWeek, _weeklyScheduleFlow.value[dayOfWeek]
            !!.copy(fromTime = newFromTime))
    }

    fun updateDayToTime(dayOfWeek: DayOfWeek, newToTime: String) {
        updateDayScheduleModel(dayOfWeek, _weeklyScheduleFlow.value[dayOfWeek]
            !!.copy(toTime = newToTime))
    }


    fun updateDayScheduleModel(dayOfWeek: DayOfWeek, newScheduleModel: DayScheduleModel) {
        _weeklyScheduleFlow.update { schedule: Map<DayOfWeek, DayScheduleModel> ->
            schedule.toMutableMap().apply {
                this[dayOfWeek] = newScheduleModel
            }
        }

        onSave(weeklyScheduleFlow.value)
    }

}