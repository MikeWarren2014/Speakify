package com.mikewarren.speakify.viewsAndViewModels.pages.scheduling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.models.scheduling.DayScheduleModel
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import com.mikewarren.speakify.data.models.scheduling.StatusSectionViewModel
import com.mikewarren.speakify.data.models.scheduling.WeeklyScheduleViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class SchedulingViewModel @Inject constructor(private val schedulingRepository: SchedulingRepository) : ViewModel() {

    var childStatusSectionViewModel by mutableStateOf<StatusSectionViewModel?>(null)
    var childWeeklyScheduleViewModel by mutableStateOf<WeeklyScheduleViewModel?>(null)

    init {
        viewModelScope.launch {
            // We do this, to make sure the status is up-to-date
            schedulingRepository.refreshSchedulingStatus()

            val initialModel = schedulingRepository.scheduling.first()

            childStatusSectionViewModel = StatusSectionViewModel(
                initialStatus = initialModel.statusModel,
                onSave = { newStatus: StatusModel ->
                    viewModelScope.launch {
                        val currentModel = schedulingRepository.scheduling.first()
                        schedulingRepository.updateScheduling(currentModel.copy(statusModel = newStatus))
                    }
                })
            
            childWeeklyScheduleViewModel = WeeklyScheduleViewModel(
                initialSchedule = initialModel.weeklySchedule,
                onSave = { newSchedule: Map<DayOfWeek, DayScheduleModel> ->
                    viewModelScope.launch {
                        val currentModel = schedulingRepository.scheduling.first()
                        schedulingRepository.updateScheduling(currentModel.copy(weeklySchedule = newSchedule))
                    }
                })
        }
    }
}
