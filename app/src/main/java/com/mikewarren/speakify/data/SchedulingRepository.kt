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
    val scheduling: Flow<SchedulingModel> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.scheduling
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

    fun getUpdatedScheduling(schedulingModel: SchedulingModel) : SchedulingModel {
        // get the time of day, and the day of the week
        val currentTimeMillis = System.currentTimeMillis()
        val dayOfWeek = LocalDateTime.now(ZoneId.systemDefault())
            .dayOfWeek

        // check the schedule for that day
        val daySchedule = schedulingModel.weeklySchedule[dayOfWeek]
            ?: return schedulingModel

        if (daySchedule.type == DayScheduleType.ALL_DAY)
            return schedulingModel.copy(statusModel = StatusModel.On)

        if (daySchedule.type == DayScheduleType.OFF)
            return schedulingModel.copy(statusModel = StatusModel.Off())

        val fromDateTime = TimeUtils.GetLocalDateTimeFrom(dayOfWeek,
            daySchedule.fromTime)
        val toDateTime = TimeUtils.GetLocalDateTimeFrom(dayOfWeek,
            daySchedule.toTime)

        // 1. Get the current system offset
        val zoneId = ZoneId.systemDefault()
        val offset = zoneId.rules.getOffset(Instant.now())

        // 2. Convert your LocalDateTime to an Epoch Milli to compare with currentTime
        val fromMillis = fromDateTime.toInstant(offset).toEpochMilli()
        var toMillis = toDateTime.toInstant(offset).toEpochMilli()

        // NOTE: this is ONLY used in the overnight case (e.g. 10 AM to 1 AM)
        if (toMillis < fromMillis) {
            toMillis += Constants.OneDay
        }

        val nextMillis = if (currentTimeMillis < fromMillis) {
            fromMillis
        } else {
            toMillis
        }

        // 3. Logic to determine if we are inside the window
        val isWithinSchedule = currentTimeMillis in fromMillis..toMillis

        if (isWithinSchedule) {
            return schedulingModel.copy(statusModel = StatusModel.On)
        }

        return schedulingModel.copy(statusModel = StatusModel.Off(nextMillis))
    }
}