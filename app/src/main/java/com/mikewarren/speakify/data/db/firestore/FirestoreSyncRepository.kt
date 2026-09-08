package com.mikewarren.speakify.data.db.firestore

import com.google.firebase.auth.FirebaseAuth
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.TrialStatus
import com.mikewarren.speakify.utils.SearchUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val trialRepository: TrialRepository,

    private val uploadRepository: UploadRepository,
    private val downloadRepository: DownloadRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val syncMutex = Mutex()
    private var observerJob: Job? = null
    private var isReadyToUpload = false
    private val firebaseAuth = FirebaseAuth.getInstance()

    init {
        startObservingChanges()
    }

    @OptIn(FlowPreview::class)
    private fun startObservingChanges() {
        observerJob?.cancel()
        observerJob = scope.launch {

            // Combine all settings into a single flow and debounce to avoid rapid-fire uploads
            combine(
                settingsRepository.useDarkTheme,
                settingsRepository.selectedTTSVoice,
                settingsRepository.maximizeVolumeOnScreenOff,
                settingsRepository.minVolume,
                settingsRepository.isCrashlyticsEnabled,
                settingsRepository.appSettings,
                appsRepository.importantApps,
                messengerContactsRepository.recentContacts,
                onboardingRepository.onboardingModel,
                trialRepository.trialModelFlow
            ) { args -> args.toList() } // Convert to list to ensure distinctUntilChanged works on content
                .drop(1)
                .debounce(2000)
                .distinctUntilChanged()
                .collectLatest {
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        val trialStatus = trialRepository.trialModelFlow.first().status
                        val isEligibleStatus = SearchUtils.IsAnyOf(trialStatus, listOf(TrialStatus.Active::class, TrialStatus.NotNeeded::class))

                        if (isEligibleStatus) {
                            if (firebaseUser.isAnonymous) {
                                // Guests are always ready to upload stats
                                isReadyToUpload = true
                            }

                            if (isReadyToUpload) {
                                uploadAllData()
                            }
                        }
                    }
                }
        }
    }

    /**
     * Uploads all local settings and app configurations to Firestore.
     */
    suspend fun uploadAllData(): Result<Unit> = syncMutex.withLock {
        return uploadRepository.doAllFirestoreTransactions()
    }

    /**
     * Downloads data from Firestore and restores it to local Room DB and DataStore.
     */
    suspend fun downloadAndRestoreData(): Result<Unit> = syncMutex.withLock {
        // Block uploads until we have successfully restored data at least once in this session.
        isReadyToUpload = false
        
        // Cancel the observer to prevent it from triggering an upload during the download/restore process
        observerJob?.cancel()

        return try {
            val result = downloadRepository.doAllFirestoreTransactions()
            if (result.isSuccess) {
                isReadyToUpload = true
            }
            result
        } finally {
            // Restart the observer after restore is complete.
            // The initial state (post-restore) will be dropped by .drop(1)
            startObservingChanges()
        }
    }
}
