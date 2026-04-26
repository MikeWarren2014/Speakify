package com.mikewarren.speakify.services

import android.content.Context
import android.media.AudioManager
import com.mikewarren.speakify.data.SettingsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeakifyAudioManagerTest {

    private lateinit var audioManager: AudioManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context
    private lateinit var speakifyAudioManager: SpeakifyAudioManager

    @Before
    fun setUp() {
        context = mockk()
        audioManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        
        // Default behavior for audioManager
        every { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) } returns 15
        every { audioManager.isMusicActive } returns false

        speakifyAudioManager = SpeakifyAudioManager(context, settingsRepository)
    }

    @Test
    fun `restoreVolume should return current volume when originalVolume is not set`() = runTest {
        // Given
        val currentVolume = 12
        every { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns currentVolume
        every { settingsRepository.originalVolume } returns flowOf(-1) // Not set yet
        every { settingsRepository.minVolume } returns flowOf(1)

        // When
        speakifyAudioManager.restoreVolume()

        // Then
        // Verify that setStreamVolume was NOT called since originalVolume was -1
        coVerify(exactly = 0) { audioManager.setStreamVolume(any(), any(), any()) }
        
        val actualVolume = speakifyAudioManager.getVolume()
        assertEquals(currentVolume, actualVolume)
    }

    @Test
    fun `restoreVolume should restore to current volume if it was increased during notification`() = runTest {
        // Given
        val savedOriginalVolume = 4
        val volumeIncreasedByUser = 8

        // originalVolume is saved as 4
        every { settingsRepository.originalVolume } returns flowOf(savedOriginalVolume)
        
        // But the user has since increased the volume to 8 (on their own or via system)
        every { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns volumeIncreasedByUser

        // When
        speakifyAudioManager.restoreVolume()

        // Then
        // We expect it NOT to restore to 4, because 8 is higher/current.
        // Actually, the requirement is to not revert to a LOWER volume than what the user set in the meantime.
        // Or if originalVolume < currentVolume, we should probably keep currentVolume.
        
        // If we want to strictly reflect the user's issue: 
        // "The volume gets restored to 4 instead of 8."
        // This implies we SHOULD NOT call setStreamVolume(..., 4, ...) if current is 8.
        
        coVerify(exactly = 0) { audioManager.setStreamVolume(any(), any(), any()) }
    }

    @Test
    fun `restoreVolume should restore to original volume if current volume is lower`() = runTest {
        // Given
        val savedOriginalVolume = 10
        val currentVolume = 4
        every { settingsRepository.originalVolume } returns flowOf(savedOriginalVolume)
        every { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns currentVolume

        // When
        speakifyAudioManager.restoreVolume()

        // Then
        coVerify(exactly = 1) { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedOriginalVolume, 0) }
    }
}
