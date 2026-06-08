package com.mikewarren.speakify.data

import android.util.Log
import androidx.datastore.core.DataStore
import com.clerk.api.Clerk
import com.mikewarren.speakify.data.db.firestore.BaseFirestoreRepository
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.utils.DeviceIdProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrialRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) : BaseFirestoreRepository(), TrialRepository {

    private val trialCollection by lazy { firestore.collection("trials") }
    private val directSignUpCollection by lazy { firestore.collection("directSignUps") }

    private val _isNewDirectSignUp = MutableStateFlow(false)
    override val isNewDirectSignUp: Flow<Boolean> = _isNewDirectSignUp.asStateFlow()

    override val trialModelFlow: Flow<TrialModel> by lazy {
        userSettingsDataStore.data
            .map { it.trialModel }
    }

    override suspend fun updateTrialModel(trialModel: TrialModel) {
        Log.d("TrialRepo", "Updating trial model in DataStore to: ${trialModel.status}")
        userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(trialModel = trialModel)
        }
    }

    override suspend fun refreshTrialStatus() {
        Log.d("TrialRepo", "refreshTrialStatus started")
        val localTrialModel = trialModelFlow.first()
        Log.d("TrialRepo", "localTrialModel status: ${localTrialModel.status}")

        if (localTrialModel.status in listOf(TrialStatus.NotNeeded, TrialStatus.Expired)) {
            Log.d("TrialRepo", "Status is NotNeeded or Expired, returning")
            return
        }

        if (Clerk.user != null) {
            Log.d("TrialRepo", "User logged in, recording sign up from refresh")
            recordSignUp()
            return
        }

        val deviceId = deviceIdProvider.deviceId
        Log.d("TrialRepo", "Checking direct sign up for device: $deviceId")
        
        // Check for direct sign-up document
        try {
            val directSignUpDoc = safeFirestoreCall {
                directSignUpCollection.document(deviceId).get().await()
            }
            if (directSignUpDoc.exists()) {
                Log.d("TrialRepo", "Direct sign up found in Firestore")
                updateTrialModel(TrialModel(status = TrialStatus.NotNeeded))
                return
            }
        } catch (e: Exception) {
            Log.e("TrialRepository", "Failed to check direct sign-up", e)
        }

        var currentTrialModel = localTrialModel

        if (currentTrialModel.startTimestamp == 0L) {
            try {
                val doc = safeFirestoreCall {
                    trialCollection.document(deviceId).get().await()
                }
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

    override suspend fun startTrial(): Result<Unit> = withContext(NonCancellable) {
        val now = System.currentTimeMillis()
        recordTrialModel(TrialModel(startTimestamp = now,
            status = TrialStatus.Active(Constants.TrialNumberOfDays)))
    }

    override suspend fun recordSignUp(): Result<Unit> = withContext(NonCancellable) {
        val localTrialModel = trialModelFlow.first()
        if (localTrialModel.status == TrialStatus.NotNeeded) {
            return@withContext Result.success(Unit)
        }
        
        if (localTrialModel.startTimestamp == 0L) {
            return@withContext recordDirectSignUp()
        }

        convertToFullVersion()
    }

    override suspend fun convertToFullVersion(): Result<Unit> = withContext(NonCancellable) {
        val localTrialModel = trialModelFlow.first()
        recordTrialModel(localTrialModel.copy(status = TrialStatus.NotNeeded))
    }

    override suspend fun recordDirectSignUp(): Result<Unit> = withContext(NonCancellable) {
        Log.d("TrialRepo", "recordDirectSignUp started")
        val user = Clerk.user ?: return@withContext Result.failure(Exception("User not logged in"))
        val deviceId = deviceIdProvider.deviceId
        
        try {
            Log.d("TrialRepo", "Checking Firestore for direct sign up doc")
            val directSignUpDoc = safeFirestoreCall {
                directSignUpCollection.document(deviceId).get().await()
            }
            if (directSignUpDoc.exists()) {
                Log.d("TrialRepo", "Direct sign up doc already exists")
                updateTrialModel(TrialModel(status = TrialStatus.NotNeeded))
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("TrialRepo", "Failed to check direct sign up doc", e)
            return@withContext Result.failure(e)
        }

        return@withContext try {
            Log.d("TrialRepo", "Recording new direct sign up in Firestore")
            safeFirestoreCall {
                directSignUpCollection.document(deviceId)
                    .set(mapOf(
                        "userEmail" to user.emailAddresses.first().emailAddress,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    .await()
            }
            
            Log.d("TrialRepo", "Firestore record successful, updating local model and flag")
            updateTrialModel(TrialModel(status = TrialStatus.NotNeeded))
            _isNewDirectSignUp.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TrialRepo", "Failed to record new direct sign up", e)
            Result.failure(e)
        }
    }

    override suspend fun endTrial(): Result<Unit> {
        val deviceId = deviceIdProvider.deviceId
        return try {
            safeFirestoreCall {
                trialCollection.document(deviceId).delete().await()
            }
            settingsRepository.clearAllData()

            updateTrialModel(trialModelFlow.first().copy(status = TrialStatus.Expired))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun resetNewDirectSignUp() {
        _isNewDirectSignUp.value = false
    }

    private suspend fun recordTrialModel(trialModel: TrialModel): Result<Unit> {
        val deviceId = deviceIdProvider.deviceId
        return try {
            safeFirestoreCall {
                trialCollection.document(deviceId)
                    .set(trialModel)
                    .await()
            }
            
            updateTrialModel(trialModel)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
