package com.mikewarren.speakify.services

import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.delegates.SimpleScheduleTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeakifyEngineGatekeeperTest: SimpleScheduleTest() {
    private lateinit var settingsRepository: SettingsRepository

    private val trialRepository = mockk<TrialRepository>(relaxed = true)
    private lateinit var gatekeeper: SpeakifyEngineGatekeeper

    @Before
    fun setUp() {
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.requireAuthenticationForSpeakifications } returns flowOf(false)

        gatekeeper = SpeakifyEngineGatekeeper(schedulingRepository, trialRepository, settingsRepository)
    }

    @Test
    fun `canSpeakNow() should return false when outside schedule and stay false on subsequent calls`() = runTest(UnconfinedTestDispatcher()) {


        val rightAfterTurnOffTime = LocalDateTime.now()
            .withHour(23)
            .withMinute(40)
        setTime(rightAfterTurnOffTime)
        // First call should update status to Off
        val firstCall = gatekeeper.canSpeakNow()
        assertFalse("First call should be false as 11:30 is outside 14:00-23:00", firstCall)

        // Second call should NOT flip back to On
        val secondCall = gatekeeper.canSpeakNow()
        assertFalse("Second call should still be false", secondCall)
    }

    @Test
    fun `canSpeakNow() should return true when inside schedule`() = runTest(UnconfinedTestDispatcher()) {
        // Change schedule to include "now" (~11:30 AM)
        val rightBeforeNoon = LocalDateTime.now()
            .withHour(11)
            .withMinute(30)
        setTime(rightBeforeNoon)

        val onSchedule = scheduleForEachDay.copy(
            fromTime = "07:00",
            toTime = "23:00"
        )
        schedulingFlow.value = schedulingFlow.value.copy(
            weeklySchedule = schedulingFlow.value.weeklySchedule.mapValues { onSchedule }
        )

        val speakificationsOn = gatekeeper.canSpeakNow()
        assertTrue("Should be true as 11:30 is inside 07:00-23:00", speakificationsOn)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}