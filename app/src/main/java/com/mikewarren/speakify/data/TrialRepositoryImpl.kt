package com.mikewarren.speakify.data

import android.util.Log
import androidx.datastore.core.DataStore
import com.clerk.api.Clerk
import com.google.firebase.firestore.FirebaseFirestore
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.utils.DeviceIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrialRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) : TrialRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val trialCollection = firestore.collection("trials")
    private val directSignUpCollection = firestore.collection("directSignUps")

    override val trialModelFlow: Flow<TrialModel> = userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            Log.d("TrialRepo", "trialModelFlow emitting: ${model.trialModel.status}")
            model.trialModel
        }

    override suspend fun updateTrialModel(trialModel: TrialModel) {
        Log.d("TrialRepo", "Updating trial model in DataStore to: ${trialModel.status}")
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(trialModel = trialModel)
        }
    }

    override suspend fun refreshTrialStatus() {
        val localTrialModel = trialModelFlow.first()

        if (localTrialModel.status in listOf(TrialStatus.NotNeeded, TrialStatus.Expired)) {
            return
        }

        if (Clerk.user != null) {
            recordSignUp()
            return
        }

        val deviceId = deviceIdProvider.deviceId
        
        // Check for direct sign-up document
        try {
            val directSignUpDoc = directSignUpCollection.document(deviceId).get().await()
            if (directSignUpDoc.exists()) {
                updateTrialModel(TrialModel(status = TrialStatus.NotNeeded))
                return
            }
        } catch (e: Exception) {
            Log.e("TrialRepository", "Failed to check direct sign-up", e)
        }

        var currentTrialModel = localTrialModel

        if (currentTrialModel.startTimestamp == 0L) {
            try {
                val doc = trialCollection.document(deviceId).get().await()
                if (doc.exists()) {
                    currentTrialModel = doc.toObject(TrialModel::class.java) ?: TrialModel()
                    if (currentTrialModel.startTimestamp != 0L) {
                        updateTrialModel(currentTrialModel)
                    }
                }
            } catch (e: Exception) {
                Log.e("TrialRepository", "Failed to refresh trial status from Firestore", e)
            }
        }

        if (currentTrialModel.status in listOf(TrialStatus.NotNeeded, TrialStatus.NotStarted)) {
            return
        }

        val now = System.currentTimeMillis()
        val diff = now - currentTrialModel.startTimestamp

        val daysPassed = TimeUnit.MILLISECONDS.toDays(diff).toInt()
        if (daysPassed >= Constants.TrialNumberOfDays) {
            updateTrialModel(currentTrialModel.copy(status = TrialStatus.Expired))
            return
        }
        updateTrialModel(currentTrialModel.copy(status = TrialStatus.Active(Constants.TrialNumberOfDays - daysPassed)))
    }

    override suspend fun startTrial(): Result<Unit> {
        val now = System.currentTimeMillis()
        return recordTrialModel(TrialModel(startTimestamp = now,
            status = TrialStatus.Active(Constants.TrialNumberOfDays)))
    }

    override suspend fun recordSignUp(): Result<Unit> {
        val localTrialModel = trialModelFlow.first()
        if (localTrialModel.status == TrialStatus.NotNeeded) {
            return Result.success(Unit)
        }
        
        if (localTrialModel.startTimestamp == 0L) {
            return recordDirectSignUp()
        }

        return convertToFullVersion()
    }

    override suspend fun convertToFullVersion(): Result<Unit> {
        val localTrialModel = trialModelFlow.first()
        return recordTrialModel(localTrialModel.copy(status = TrialStatus.NotNeeded))
    }

    override suspend fun recordDirectSignUp(): Result<Unit> {
        val user = Clerk.user ?: return Result.failure(Exception("User not logged in"))
        val deviceId = deviceIdProvider.deviceId
        
        if (directSignUpCollection.document(deviceId).get().await().exists()) {
            updateTrialModel(TrialModel(status = TrialStatus.NotNeeded))
            return Result.success(Unit)
        }

        return try {
            directSignUpCollection.document(deviceId)
                .set(mapOf(
                    "userEmail" to user.emailAddresses.first().emailAddress,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            updateTrialModel(TrialModel(status = TrialStatus.NotNeeded))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun endTrial(): Result<Unit> {
        val deviceId = deviceIdProvider.deviceId
        return try {
            trialCollection.document(deviceId).delete().await()
            settingsRepository.clearAllData()

            updateTrialModel(trialModelFlow.first().copy(status = TrialStatus.Expired))
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
            
            updateTrialModel(trialModel)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
