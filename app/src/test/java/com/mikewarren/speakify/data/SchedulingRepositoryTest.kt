package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.delegates.SimpleScheduleTest
import com.mikewarren.speakify.data.models.scheduling.DayScheduleType
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchedulingRepositoryTest: SimpleScheduleTest() {


    @Test
    fun `getUpdatedScheduling() should return On if status is Off but turnOnTime is specified and has passed`() {
        val timeWithinSchedule = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday
        setTime(timeWithinSchedule)

        val turnOnTime = getMillis(timeWithinSchedule.minusHours(1))

        val model = schedulingFlow.value.copy(statusModel = StatusModel.Off(turnOnTime))
        val result = schedulingRepository.getUpdatedScheduling(model)

        assertTrue(result.statusModel is StatusModel.On)
    }

    @Test
    fun `getUpdatedScheduling() should return Off if status is Off and turnOnTime is in future`() {
        val offTime = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday
        setTime(offTime)

        val turnOnLocalDateTime = offTime.plusHours(1)
        val turnOnTime = getMillis(turnOnLocalDateTime)

        val model = schedulingFlow.value.copy(statusModel = StatusModel.Off(turnOnTime))
        val result = schedulingRepository.getUpdatedScheduling(model)

        assertTrue(result.statusModel is StatusModel.Off)

        val actualTurnOnTime = (result.statusModel as StatusModel.Off).turnOnTime
        assertTime(actualTurnOnTime!!, turnOnLocalDateTime.hour, turnOnLocalDateTime.minute)
    }

    @Test
    fun `getUpdatedScheduling() should return On when day type is ALL_DAY`() {
        val allDaySchedule = scheduleForEachDay.copy(type = DayScheduleType.ALL_DAY)
        val model = schedulingFlow.value.copy(
            weeklySchedule = schedulingFlow.value.weeklySchedule + (DayOfWeek.FRIDAY to allDaySchedule)
        )

        val now = LocalDateTime.of(2023, 10, 27, 2, 0) // Friday 2 AM
        setTime(now)

        val result = schedulingRepository.getUpdatedScheduling(model)
        assertTrue(result.statusModel is StatusModel.On)
    }

    @Test
    fun `getUpdatedScheduling() should return Off when day type is OFF`() {
        val offSchedule = scheduleForEachDay.copy(type = DayScheduleType.OFF)
        val model = schedulingFlow.value.copy(
            weeklySchedule = schedulingFlow.value.weeklySchedule + (DayOfWeek.FRIDAY to offSchedule)
        )

        val now = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday 10 AM
        setTime(now)

        val result = schedulingRepository.getUpdatedScheduling(model)
        assertTrue(result.statusModel is StatusModel.Off)
    }

    @Test
    fun `getUpdatedScheduling() should return On when within SELECT_TIMES window`() {
        val onTime = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday 10 AM
        setTime(onTime)

        // Schedule: 07:00 to 23:30 (now is 10:00)
        val result = schedulingRepository.getUpdatedScheduling(schedulingFlow.value)

        assertTrue(result.statusModel is StatusModel.On)
    }

    @Test
    fun `getUpdatedScheduling() should return Off when before SELECT_TIMES window`() {
        val beforeOnTime = LocalDateTime.of(2023, 10, 27, 6, 0) // Friday 6 AM
        setTime(beforeOnTime)

        // Schedule: 07:00 to 23:30
        val result = schedulingRepository.getUpdatedScheduling(schedulingFlow.value)

        assertTrue(result.statusModel is StatusModel.Off)
        val turnOnTime = (result.statusModel as StatusModel.Off).turnOnTime
        assertTime(turnOnTime!!, 7, 0)
    }

    @Test
    fun `getUpdatedScheduling() should return Off with turnOnTime as today's end time if past the window`() {
        val justAfterTurnOffTime = LocalDateTime.of(2023, 10, 27, 23, 45) // Friday 11:45 PM
        setTime(justAfterTurnOffTime)

        // Schedule: 07:00 to 23:30
        val result = schedulingRepository.getUpdatedScheduling(schedulingFlow.value)

        assertTrue(result.statusModel is StatusModel.Off)
        val turnOnTime = (result.statusModel as StatusModel.Off).turnOnTime
        assertTime(turnOnTime!!, 7, 0)
    }

    @Test
    fun `getUpdatedScheduling() should handle overnight SELECT_TIMES window`() {
        // Overnight schedule: Friday 10 PM to 6 AM
        val overnightSchedule = scheduleForEachDay.copy(fromTime = "22:00", toTime = "06:00")
        val model = schedulingFlow.value.copy(
            weeklySchedule = schedulingFlow.value.weeklySchedule + (DayOfWeek.FRIDAY to overnightSchedule)
        )

        // Friday 11 PM (Within window)
        val nowWithin = LocalDateTime.of(2023, 10, 27, 23, 0)
        setTime(nowWithin)
        val resultWithin = schedulingRepository.getUpdatedScheduling(model)
        assertTrue(resultWithin.statusModel is StatusModel.On)
    }
}
