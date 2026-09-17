package com.mikewarren.speakify.data.delegates

import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.UserSettingsModel
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneId

interface IScheduleTest {
    val schedulingRepository: SchedulingRepository
    val userSettingsDataStore: DataStore<UserSettingsModel>

    val schedulingFlow: MutableStateFlow<SchedulingModel>

    fun getMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun setTime(now: LocalDateTime) {
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns now
    }
}