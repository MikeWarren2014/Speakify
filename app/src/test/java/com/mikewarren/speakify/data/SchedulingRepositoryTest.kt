package com.mikewarren.speakify.data

import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.models.scheduling.DayScheduleModel
import com.mikewarren.speakify.data.models.scheduling.DayScheduleType
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchedulingRepositoryTest {
    private val scheduleForEachDay = DayScheduleModel(
        dayName = "",
        type = DayScheduleType.SELECT_TIMES,
        fromTime = "07:00",
        toTime = "23:30",
    )

    private val schedulingFlow: MutableStateFlow<SchedulingModel> = MutableStateFlow(SchedulingModel(
        statusModel = StatusModel.On,
        weeklySchedule = mapOf(
            DayOfWeek.SATURDAY to scheduleForEachDay.copy(dayName = "Saturday"),
            DayOfWeek.SUNDAY to scheduleForEachDay.copy(dayName = "Sunday"),
            DayOfWeek.MONDAY to scheduleForEachDay.copy(dayName = "Monday"),
            DayOfWeek.TUESDAY to scheduleForEachDay.copy(dayName = "Tuesday"),
            DayOfWeek.WEDNESDAY to scheduleForEachDay.copy(dayName = "Wednesday"),
            DayOfWeek.THURSDAY to scheduleForEachDay.copy(dayName = "Thursday"),
            DayOfWeek.FRIDAY to scheduleForEachDay.copy(dayName = "Friday"),
        )
    ))

    private val userSettingsDataStore: DataStore<UserSettingsModel> = mockk(relaxed = true)
    private val schedulingRepository: SchedulingRepository = SchedulingRepository(
        userSettingsDataStore = userSettingsDataStore,
    )

    private fun getMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @Test
    fun `getUpdatedScheduling() should return On if status is Off but turnOnTime has passed`() {
        val now = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday
        val turnOnTime = getMillis(now.minusHours(1))

        val model = schedulingFlow.value.copy(statusModel = StatusModel.Off(turnOnTime))
        val result = schedulingRepository.getUpdatedScheduling(model, now)

        assertTrue(result.statusModel is StatusModel.On)
    }

    @Test
    fun `getUpdatedScheduling() should return Off if status is Off and turnOnTime is in future`() {
        val now = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday
        val turnOnTime = getMillis(now.plusHours(1))

        val model = schedulingFlow.value.copy(statusModel = StatusModel.Off(turnOnTime))
        val result = schedulingRepository.getUpdatedScheduling(model, now)

        assertTrue(result.statusModel is StatusModel.Off)
        assertEquals(turnOnTime, (result.statusModel as StatusModel.Off).turnOnTime)
    }

    @Test
    fun `getUpdatedScheduling() should return On when day type is ALL_DAY`() {
        val allDaySchedule = scheduleForEachDay.copy(type = DayScheduleType.ALL_DAY)
        val model = schedulingFlow.value.copy(
            weeklySchedule = schedulingFlow.value.weeklySchedule + (DayOfWeek.FRIDAY to allDaySchedule)
        )

        val now = LocalDateTime.of(2023, 10, 27, 2, 0) // Friday 2 AM
        val result = schedulingRepository.getUpdatedScheduling(model, now)
        assertTrue(result.statusModel is StatusModel.On)
    }

    @Test
    fun `getUpdatedScheduling() should return Off when day type is OFF`() {
        val offSchedule = scheduleForEachDay.copy(type = DayScheduleType.OFF)
        val model = schedulingFlow.value.copy(
            weeklySchedule = schedulingFlow.value.weeklySchedule + (DayOfWeek.FRIDAY to offSchedule)
        )

        val now = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday 10 AM
        val result = schedulingRepository.getUpdatedScheduling(model, now)
        assertTrue(result.statusModel is StatusModel.Off)
    }

    @Test
    fun `getUpdatedScheduling() should return On when within SELECT_TIMES window`() {
        val now = LocalDateTime.of(2023, 10, 27, 10, 0) // Friday 10 AM

        // Schedule: 07:00 to 23:30 (now is 10:00)
        val result = schedulingRepository.getUpdatedScheduling(schedulingFlow.value, now)

        assertTrue(result.statusModel is StatusModel.On)
    }

    @Test
    fun `getUpdatedScheduling() should return Off when before SELECT_TIMES window`() {
        val now = LocalDateTime.of(2023, 10, 27, 6, 0) // Friday 6 AM

        // Schedule: 07:00 to 23:30
        val result = schedulingRepository.getUpdatedScheduling(schedulingFlow.value, now)

        assertTrue(result.statusModel is StatusModel.Off)
        val turnOnTime = (result.statusModel as StatusModel.Off).turnOnTime
        val expectedTurnOn = getMillis(LocalDateTime.of(2023, 10, 27, 7, 0))
        assertEquals(expectedTurnOn, turnOnTime)
    }

    @Test
    fun `getUpdatedScheduling() should return Off with turnOnTime as today's end time if past the window`() {
        val now = LocalDateTime.of(2023, 10, 27, 23, 45) // Friday 11:45 PM

        // Schedule: 07:00 to 23:30
        val result = schedulingRepository.getUpdatedScheduling(schedulingFlow.value, now)

        assertTrue(result.statusModel is StatusModel.Off)
        val turnOnTime = (result.statusModel as StatusModel.Off).turnOnTime
        val expectedTurnOn = getMillis(LocalDateTime.of(2023, 10, 27, 23, 30))
        assertEquals(expectedTurnOn, turnOnTime)
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
        val resultWithin = schedulingRepository.getUpdatedScheduling(model, nowWithin)
        assertTrue(resultWithin.statusModel is StatusModel.On)
    }
}
