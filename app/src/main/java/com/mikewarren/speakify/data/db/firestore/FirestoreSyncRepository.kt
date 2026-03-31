package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.TrialStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val messengerContactsRepository: MessengerContactsRepository,

    private val trialRepository: TrialRepository,

    private val uploadRepository: UploadRepository,
    private val downloadRepository: DownloadRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        startObservingChanges()
    }

    @OptIn(FlowPreview::class)
    private fun startObservingChanges() {
        scope.launch {
            if (trialRepository.trialStatus.first() is TrialStatus.Active)
                return@launch

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
            ) { args -> args }
                .drop(1)
                .debounce(2000)
                .distinctUntilChanged()
                .collectLatest {
                    uploadAllData()
                }
        }
    }

    /**
     * Uploads all local settings and app configurations to Firestore.
     */
    suspend fun uploadAllData(): Result<Unit> {
        return uploadRepository.doAllFirestoreTransactions()
    }

    /**
     * Downloads data from Firestore and restores it to local Room DB and DataStore.
     */
    suspend fun downloadAndRestoreData(): Result<Unit> {
        return downloadRepository.doAllFirestoreTransactions()
    }
}
