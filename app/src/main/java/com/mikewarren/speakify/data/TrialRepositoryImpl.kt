package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.google.firebase.firestore.FirebaseFirestore
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.utils.DeviceIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrialRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deviceIdProvider: DeviceIdProvider
) : TrialRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val trialCollection = firestore.collection("trials")

    private val _trialStatus = MutableStateFlow<TrialStatus>(TrialStatus.Loading)
    override val trialStatus: Flow<TrialStatus> = _trialStatus.asStateFlow()

    override suspend fun refreshTrialStatus() {
        val localTrialModel = settingsRepository.trialModel.first()

        if (localTrialModel.isConverted) {
            _trialStatus.value = TrialStatus.NotNeeded
            return
        }

        if (Clerk.user != null) {
            _trialStatus.value = TrialStatus.NotNeeded
            return
        }

        val deviceId = deviceIdProvider.deviceId
        var currentTrialModel = localTrialModel

        if (currentTrialModel.startTimestamp == 0L) {
            // 2. Check Firestore if not set locally
            try {
                val doc = trialCollection.document(deviceId).get().await()
                if (doc.exists()) {
                    val start = doc.getLong("startTimestamp") ?: 0L
                    val converted = doc.getBoolean("isConverted") ?: false
                    currentTrialModel = TrialModel(start, converted)
                    if (start != 0L) {
                        settingsRepository.updateTrialModel(currentTrialModel)
                    }
                }
            } catch (e: Exception) {
                Log.e("TrialRepository", "Failed to refresh trial status from Firestore", e)
            }
        }

        if (currentTrialModel.isConverted) {
            _trialStatus.value = TrialStatus.NotNeeded
            return
        }

        if (currentTrialModel.startTimestamp == 0L) {
            _trialStatus.value = TrialStatus.NotStarted
            return
        }

        val now = System.currentTimeMillis()
        val diff = now - currentTrialModel.startTimestamp

        val daysPassed = TimeUnit.MILLISECONDS.toDays(diff).toInt()
        if (daysPassed >= Constants.TrialNumberOfDays) {
            _trialStatus.value = TrialStatus.Expired
            return
        }
        _trialStatus.value = TrialStatus.Active(Constants.TrialNumberOfDays - daysPassed)
    }

    override suspend fun startTrial(): Result<Unit> {
        val now = System.currentTimeMillis()
        return recordTrialModel(TrialModel(startTimestamp = now, isConverted = false))
    }

    override suspend fun recordDeviceActivity() {
        val localTrialModel = settingsRepository.trialModel.first()
        if (localTrialModel.startTimestamp == 0L) {
            // If we don't have a start timestamp yet (e.g. they just signed up/in without a trial),
            // we should set one now to mark the beginning of their relationship with this device.
            recordTrialModel(TrialModel(startTimestamp = System.currentTimeMillis(), isConverted = false))
            return
        }
        // Even if we have one, ensure Firestore is synced with this device ID
        val deviceId = deviceIdProvider.deviceId
        try {
            trialCollection.document(deviceId)
                .set(localTrialModel)
                .await()
        } catch (e: Exception) {
            // Ignore failures here as it's a side-effect
            Log.e("TrialRepository", "Failed to record device activity", e)
        }
    }

    override suspend fun convertToFullVersion(): Result<Unit> {
        val localTrialModel = settingsRepository.trialModel.first()
        if (localTrialModel.startTimestamp == 0L)
            return Result.success(Unit)

        return recordTrialModel(localTrialModel.copy(isConverted = true))
    }

    override suspend fun endTrial(): Result<Unit> {
        val deviceId = deviceIdProvider.deviceId
        return try {
            trialCollection.document(deviceId).delete().await()
            settingsRepository.clearAllData()
            _trialStatus.value = TrialStatus.Expired
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recordTrialModel(trialModel: TrialModel): Result<Unit> {
        val deviceId = deviceIdProvider.deviceId
        return try {
            trialCollection.document(deviceId)
                .set(trialModel)
                .await()
            
            settingsRepository.updateTrialModel(trialModel)
            refreshTrialStatus() // Refresh status
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
