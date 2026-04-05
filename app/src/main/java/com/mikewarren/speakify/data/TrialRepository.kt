package com.mikewarren.speakify.data

import kotlinx.coroutines.flow.Flow

interface TrialRepository {
    val trialStatus: Flow<TrialStatus>
    suspend fun startTrial(): Result<Unit>
    suspend fun refreshTrialStatus()
    suspend fun recordDeviceActivity()
    suspend fun convertToFullVersion(): Result<Unit>
    suspend fun recordDirectSignUp(): Result<Unit>
    suspend fun endTrial(): Result<Unit>
}

sealed interface TrialStatus {
    data object NotStarted : TrialStatus
    data class Active(val daysRemaining: Int) : TrialStatus
    data object Expired : TrialStatus
    data object Loading : TrialStatus
    data object NotNeeded: TrialStatus
}
