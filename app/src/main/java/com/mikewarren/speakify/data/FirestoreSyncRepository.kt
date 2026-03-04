package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mikewarren.speakify.data.db.UserAppModel
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
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
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
                settingsRepository.appSettings,
                appsRepository.importantApps
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
     * Uses Firebase userId as the document ID.
     */
    suspend fun uploadAllData(): Result<Unit> {
        val firebaseUid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in to Firebase"))
        val clerkUserId = Clerk.user?.id


        return try {
            val userDoc = usersCollection.document(firebaseUid)

            // 0. Associate the Firebase record with the Clerk User ID
            if (clerkUserId != null) {
                userDoc
                    .set(hashMapOf(
                        "clerkUserId" to clerkUserId,
                        "userEmail" to Clerk.user?.emailAddresses!!.first().emailAddress,
                    ), SetOptions.merge())
                    .await()
            }
            
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

            // 2. Sync App Settings (Detailed config per app)
            val appSettingsCollection = userDoc.collection("app_settings")
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
                appSettingsCollection.document(docId)
                    .set(appData, SetOptions.merge())
                    .await()
            }

            // 3. Sync Important Apps (The list of apps to listen to)
            val importantAppsCollection = userDoc.collection("important_apps")
            val importantAppsList = appsRepository.importantApps.first()
            importantAppsList.forEach { app ->
                val docId = app.packageName.replace(".", "_")
                importantAppsCollection.document(docId)
                    .set(app, SetOptions.merge())
                    .await()
            }

            Log.d("FirestoreSync", "All data uploaded successfully for user $firebaseUid")
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
        val userId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in to Firebase"))

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

            // 2. Restore App Settings
            val appSettingsSnapshot = userDoc.collection("app_settings")
                .get()
                .await()
            for (doc in appSettingsSnapshot.documents) {
                val packageName = doc.getString("packageName") ?: continue
                val announcerVoice = doc.getString("announcerVoice")
                @Suppress("UNCHECKED_CAST")
                val notificationSources = doc.get("notificationSources") as? List<String> ?: emptyList()
                
                settingsRepository.saveAppSettings(AppSettingsModel(
                    id = null,
                    packageName = packageName,
                    announcerVoice = announcerVoice,
                    notificationSources = notificationSources
                ))
            }

            // 3. Restore Important Apps
            val importantAppsSnapshot = userDoc.collection("important_apps")
                .get()
                .await()
            for (doc in importantAppsSnapshot.documents) {
                val app = doc.toObject(UserAppModel::class.java)
                if (app != null) {
                    appsRepository.addImportantApp(app)
                }
            }

            Log.d("FirestoreSync", "All data restored successfully for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to restore data", e)
            Result.failure(e)
        }
    }
}
