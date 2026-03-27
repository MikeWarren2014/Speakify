package com.mikewarren.speakify.viewsAndViewModels.pages.scheduling

import androidx.compose.animation.core.copy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.mikewarren.speakify.data.models.DayScheduleState
import com.mikewarren.speakify.data.models.DayScheduleType

class SchedulingViewModel : ViewModel() {
    var isAppOn by mutableStateOf(true)
    var pauseHours by mutableStateOf("")
    var pauseMinutes by mutableStateOf("")// Initialize schedule for all 7 days
    var weeklySchedule by mutableStateOf(
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            .map { DayScheduleState(it) }
    )

    fun toggleAppStatus() {
        isAppOn = !isAppOn
    }

    fun updateDayType(dayName: String, newType: DayScheduleType) {
        weeklySchedule = weeklySchedule.map {
            if (it.dayName == dayName) it.copy(type = newType) else it
        }
    }
}