package com.mikewarren.speakify.viewsAndViewModels.pages.scheduling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.models.scheduling.DayScheduleModel
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import com.mikewarren.speakify.data.models.scheduling.StatusSectionViewModel
import com.mikewarren.speakify.data.models.scheduling.WeeklyScheduleViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class SchedulingViewModel @Inject constructor(private val schedulingRepository: SchedulingRepository) : ViewModel() {
    val modelFlow = schedulingRepository.scheduling
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SchedulingModel()
        )

    var childStatusSectionViewModel by mutableStateOf<StatusSectionViewModel?>(null)
    var childWeeklyScheduleViewModel by mutableStateOf<WeeklyScheduleViewModel?>(null)

    init {
        viewModelScope.launch {
            // We do this, to make sure the status is up-to-date
            schedulingRepository.refreshSchedulingStatus()

            val initialModel = modelFlow.value

            childStatusSectionViewModel = StatusSectionViewModel(
                initialStatus = initialModel.statusModel,
                onSave = { newStatus: StatusModel ->
                    viewModelScope.launch {
                        schedulingRepository.updateScheduling(modelFlow.value.copy(statusModel = newStatus))
                    }
                })
            
            childWeeklyScheduleViewModel = WeeklyScheduleViewModel(
                initialSchedule = initialModel.weeklySchedule,
                onSave = { newSchedule: Map<DayOfWeek, DayScheduleModel> ->
                    viewModelScope.launch {
                        schedulingRepository.updateScheduling(modelFlow.value.copy(weeklySchedule = newSchedule))
                    }
                })
        }
    }
}
