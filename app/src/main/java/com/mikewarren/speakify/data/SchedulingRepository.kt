package com.mikewarren.speakify.data

import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.models.scheduling.DayScheduleType
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import com.mikewarren.speakify.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulingRepository @Inject constructor(
    // Inject your DataStore or DAO here
    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) {
    val scheduling: Flow<SchedulingModel> by lazy {
        userSettingsDataStore.data
            .map { it.scheduling }
    }

    suspend fun updateScheduling(scheduling: SchedulingModel) {
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(scheduling = scheduling)
        }
    }

    suspend fun refreshSchedulingStatus() {
        val schedulingModel = scheduling.first()
        updateScheduling(getUpdatedScheduling(schedulingModel))
    }

    fun getUpdatedScheduling(
        schedulingModel: SchedulingModel,
    ) : SchedulingModel {
        val now = LocalDateTime.now()
        val currentTimeMillis = now
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val statusModel = schedulingModel.statusModel

        // 1. Determine what the schedule says we SHOULD be.
        val dayOfWeek = now.dayOfWeek
        val daySchedule = schedulingModel.weeklySchedule[dayOfWeek]

        val scheduleResult: StatusModel = if (daySchedule == null || daySchedule.type == DayScheduleType.OFF) {
            StatusModel.Off(getNextStartMillis(schedulingModel, now))
        } else if (daySchedule.type == DayScheduleType.ALL_DAY) {
            StatusModel.On
        } else {
            val fromDateTime = TimeUtils.GetLocalDateTimeFrom(dayOfWeek, daySchedule.fromTime, now)
            val toDateTime = TimeUtils.GetLocalDateTimeFrom(dayOfWeek, daySchedule.toTime, now)

            val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())
            val fromMillis = fromDateTime.toInstant(offset).toEpochMilli()
            var toMillis = toDateTime.toInstant(offset).toEpochMilli()

            if (toMillis < fromMillis) {
                toMillis += Constants.OneDay
            }

            if (currentTimeMillis in fromMillis..toMillis) {
                StatusModel.On
            } else {
                StatusModel.Off(getNextStartMillis(schedulingModel, now))
            }
        }

        // 2. Resolve current status vs schedule result
        return when (statusModel) {
            is StatusModel.On -> {
                // If we are currently ON, but the schedule says OFF, we go OFF.
                if (scheduleResult is StatusModel.Off) {
                    schedulingModel.copy(statusModel = scheduleResult)
                } else {
                    schedulingModel
                }
            }
            is StatusModel.Off -> {
                val turnOnTime = statusModel.turnOnTime

                if (turnOnTime == null) {
                    // Permanent OFF manually set. Stay OFF.
                    return schedulingModel
                }

                if (currentTimeMillis < turnOnTime) {
                    // We are still in a "Pause" or scheduled OFF period.
                    // However, we should check if the schedule has since turned ON.
                    // If the schedule is now ON, and our turnOnTime was a *scheduled* one (not a manual pause),
                    // we might want to turn ON. But for simplicity, we respect the turnOnTime.
                    return schedulingModel
                }

                // turnOnTime has passed. Revert to what the schedule says.
                schedulingModel.copy(statusModel = scheduleResult)
            }
        }
    }

    private fun getNextStartMillis(schedulingModel: SchedulingModel, now: LocalDateTime): Long? {
        val currentTimeMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weeklySchedule = schedulingModel.weeklySchedule
        
        // Check today and the next 7 days
        for (i in 0..7) {
            val checkDay = now.plusDays(i.toLong())
            val dayOfWeek = checkDay.dayOfWeek
            val daySchedule = weeklySchedule[dayOfWeek] ?: continue
            
            if (daySchedule.type == DayScheduleType.OFF) continue
            
            val fromTime = if (daySchedule.type == DayScheduleType.ALL_DAY) "00:00" else daySchedule.fromTime
            val fromDateTime = TimeUtils.GetLocalDateTimeWithHHMM(checkDay, fromTime)
            val fromMillis = fromDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            if (fromMillis > currentTimeMillis) {
                return fromMillis
            }
        }
        return null
    }
}
