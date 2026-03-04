package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        startObservingChanges()
    }

    @OptIn(FlowPreview::class)
    private fun startObservingChanges() {
        scope.launch {
            // Combine all settings into a single flow and debounce to avoid rapid-fire uploads
            combine(
                settingsRepository.useDarkTheme,
                settingsRepository.selectedTTSVoice,
                settingsRepository.maximizeVolumeOnScreenOff,
                settingsRepository.minVolume,
                settingsRepository.isCrashlyticsEnabled,
                settingsRepository.appSettings
            ) { args ->
                // Just a trigger to signal something changed
                args
            }
                .drop(1) // Drop initial values to avoid immediate upload on startup
                .debounce(2000) // Wait for 2 seconds of inactivity before uploading
                .distinctUntilChanged()
                .collectLatest {
                    uploadAllData()
                }
        }
    }

    /**
     * Uploads all local settings and app configurations to Firestore.
     * Uses Clerk's userId as the document ID.
     */
    suspend fun uploadAllData(): Result<Unit> {
        val userId = Clerk.user?.id ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val userDoc = usersCollection.document(userId)
            
            // 1. Sync User Settings
            val settingsData = hashMapOf(
                "useDarkTheme" to settingsRepository.useDarkTheme.first(),
                "selectedTTSVoice" to settingsRepository.selectedTTSVoice.first(),
                "maximizeVolumeOnScreenOff" to settingsRepository.maximizeVolumeOnScreenOff.first(),
                "minVolume" to settingsRepository.minVolume.first(),
                "isCrashlyticsEnabled" to settingsRepository.isCrashlyticsEnabled.first()
            )
            userDoc.collection("config")
                .document("settings")
                .set(settingsData, SetOptions.merge())
                .await()

            // 2. Sync App Settings
            val appsCollection = userDoc.collection("apps")
            val appSettingsMap = settingsRepository.appSettings.first()
            
            // We might want to clear old apps or just merge. 
            // For now, we merge/update existing ones.
            appSettingsMap.forEach { (packageName, model) ->
                // Clean package name for document ID if necessary, though package names are usually fine
                val docId = packageName.replace(".", "_")
                // Firestore doesn't like custom classes with Long? id if not configured, 
                // so we map it to a hashmap for safety.
                val appData = hashMapOf(
                    "packageName" to model.packageName,
                    "announcerVoice" to model.announcerVoice,
                    "notificationSources" to model.notificationSources
                )
                appsCollection.document(docId)
                    .set(appData, SetOptions.merge())
                    .await()
            }

            Log.d("FirestoreSync", "All data uploaded successfully for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to upload data", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads data from Firestore and restores it to local Room DB and DataStore.
     */
    suspend fun downloadAndRestoreData(): Result<Unit> {
        val userId = Clerk.user?.id ?: return Result.failure(Exception("User not logged in"))

        return try {
            val userDoc = usersCollection.document(userId)

            // 1. Restore Settings
            val settingsSnapshot = userDoc.collection("config")
                .document("settings")
                .get()
                .await()
            if (settingsSnapshot.exists()) {
                settingsSnapshot.getBoolean("useDarkTheme")?.let {
                    settingsRepository.updateUseDarkTheme(it)
                }
                settingsSnapshot.getString("selectedTTSVoice")?.let { 
                    settingsRepository.saveSelectedVoice(it) 
                }
                settingsSnapshot.getBoolean("maximizeVolumeOnScreenOff")?.let { 
                    settingsRepository.setMaximizeVolumeOnScreenOff(it) 
                }
                settingsSnapshot.getLong("minVolume")?.let { 
                    settingsRepository.setMinVolume(it.toInt()) 
                }
                settingsSnapshot.getBoolean("isCrashlyticsEnabled")?.let { 
                    settingsRepository.setCrashlyticsEnabled(it) 
                }
            }

            // 2. Restore Apps
            val appsSnapshot = userDoc.collection("apps")
                .get()
                .await()
            for (doc in appsSnapshot.documents) {
                val packageName = doc.getString("packageName") ?: continue
                val announcerVoice = doc.getString("announcerVoice")
                @Suppress("UNCHECKED_CAST")
                val notificationSources = doc.get("notificationSources") as? List<String> ?: emptyList()
                
                val appSettings = AppSettingsModel(
                    id = null, // Repository will handle ID generation
                    packageName,
                    announcerVoice,
                    notificationSources,
                )
                settingsRepository.saveAppSettings(appSettings)
            }

            Log.d("FirestoreSync", "All data restored successfully for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to restore data", e)
            Result.failure(e)
        }
    }
}
