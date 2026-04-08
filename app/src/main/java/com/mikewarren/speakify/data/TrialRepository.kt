package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.models.TrialModel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface TrialRepository {
    val trialModelFlow: Flow<TrialModel>
    suspend fun updateTrialModel(trialModel: TrialModel)
    suspend fun startTrial(): Result<Unit>
    suspend fun refreshTrialStatus()
    suspend fun recordSignUp(): Result<Unit>
    suspend fun convertToFullVersion(): Result<Unit>
    suspend fun recordDirectSignUp(): Result<Unit>
    suspend fun endTrial(): Result<Unit>
}

@Serializable
sealed class TrialStatus(val statusText: String) {
    @Serializable
    @SerialName("NotStarted")
    data object NotStarted : TrialStatus("not started")

    @Serializable
    @SerialName("Active")
    data class Active(val daysRemaining: Int) : TrialStatus("active")

    @Serializable
    @SerialName("Expired")
    data object Expired : TrialStatus("expired")

    // TODO: I think this is YAGNI
    @Serializable
    @SerialName("Loading")
    data object Loading : TrialStatus("loading")

    @Serializable
    @SerialName("NotNeeded")
    data object NotNeeded: TrialStatus("converted")

    companion object {
        fun valueOf(statusText: String): TrialStatus {
            return when (statusText.lowercase()) {
                "active" -> Active(0)
                "expired" -> Expired
                "loading" -> Loading
                "converted" -> NotNeeded
                else -> NotStarted
            }
        }
    }
}
