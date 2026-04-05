package com.mikewarren.speakify.services

import com.clerk.api.Clerk
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.models.scheduling.StatusModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeakifyEngineGatekeeper @Inject constructor(
    private val schedulingRepository: SchedulingRepository,
    private val settingsRepository: SettingsRepository // If you have a global "Master Mute"
) {

    /**
     * The single "Green Light" for the app to speak.
     * Checks scheduling, manual pauses, and global status.
     */
    suspend fun canSpeakNow(): Boolean {
        if ((!checkAuthentication()) && (!hasStartedTheApp())) {
            return false
        }

        // 1. Force an update to ensure the StatusModel (On/Off/Off until X)
        // is accurate relative to the current clock time.
        schedulingRepository.refreshSchedulingStatus()

        // 2. Fetch the latest model
        val schedulingModel = schedulingRepository.scheduling.first()

        // 3. Simple boolean check on the resulting StatusModel
        return when (val status = schedulingModel.statusModel) {
            is StatusModel.On -> true
            is StatusModel.Off -> false
        }
    }

    fun checkAuthentication(): Boolean {
        return Clerk.user != null
    }

    suspend fun hasStartedTheApp(): Boolean {
        return settingsRepository.trialModel.first().startTimestamp > 0L
    }
}