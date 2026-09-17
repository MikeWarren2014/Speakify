package com.mikewarren.speakify.data.delegates

import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.UserSettingsModel
import com.mikewarren.speakify.data.models.scheduling.DayScheduleModel
import com.mikewarren.speakify.data.models.scheduling.DayScheduleType
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.After
import org.robolectric.shadows.ShadowSystemClock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

open class SimpleScheduleTest: IScheduleTest {
    protected val scheduleForEachDay = DayScheduleModel(
        dayName = "",
        type = DayScheduleType.SELECT_TIMES,
        fromTime = "07:00",
        toTime = "23:30",
    )

    override val schedulingFlow: MutableStateFlow<SchedulingModel> = MutableStateFlow(SchedulingModel(
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

    override val userSettingsDataStore: DataStore<UserSettingsModel> = mockk(relaxed = true)

    override val schedulingRepository: SchedulingRepository = SchedulingRepository(
        userSettingsDataStore = userSettingsDataStore,
    )

    // TODO: shouldn't this be in a setUp()?
    init {
        val userSettingsFlow = schedulingFlow.map { UserSettingsModel().copy(scheduling = it) }
        every { userSettingsDataStore.data } returns userSettingsFlow
        coEvery { userSettingsDataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (UserSettingsModel) -> UserSettingsModel>()
            val current = UserSettingsModel().copy(scheduling = schedulingFlow.value)
            val updated = transform(current)
            schedulingFlow.value = updated.scheduling
            updated
        }
    }

    fun assertTime(actualTime: Long, expectedHours: Int, expectedMinutes: Int) {
        val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())

        val actualLocalDateTime = LocalDateTime.ofEpochSecond(actualTime /1000, 0, offset)

        assertEquals(expectedHours, actualLocalDateTime.hour)
        assertEquals(expectedMinutes, actualLocalDateTime.minute)
    }
}
