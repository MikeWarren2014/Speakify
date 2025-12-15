package com.mikewarren.speakify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import android.util.Log
import android.util.LruCache
import androidx.core.app.NotificationCompat
import com.clerk.api.Clerk
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.data.events.NotificationPermissionEvent
import com.mikewarren.speakify.data.events.NotificationPermissionEventBus
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.receivers.PhoneStateReceiver
import com.mikewarren.speakify.receivers.ScreenStateReceiver
import com.mikewarren.speakify.strategies.NotificationStrategyFactory
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpeakifyNotificationListener : NotificationListenerService(), ITaggable {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userAppsDao: UserAppsDao

    @Inject
    lateinit var appSettingsDao: AppSettingsDao

    @Inject
    lateinit var notificationSourcesDao: NotificationSourcesDao

    val notificationPermissionEventBus: NotificationPermissionEventBus = NotificationPermissionEventBus.GetInstance()

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var ttsManager: TTSManager

    private lateinit var defaultVoice: String;

    private val recentlySpokenCache = LruCache<String, Long>(20)

    companion object {
        const val DEBOUNCE_TIME_MS = 5 * Constants.OneSecond
    }

    private val phoneStateReceiver = PhoneStateReceiver()
    private val screenStateReceiver = ScreenStateReceiver()

    override fun onCreate() {
        super.onCreate()
        // Start the collector when the service is created.
        // This ensures it's always listening as long as the service process is alive.
        startListeningForNotifications()

        listenForPermissionEvents()

        Log.d(TAG, "Service created. Registering PhoneStateReceiver.")

        // Define the intent filter for the broadcast we want to receive.
        val intentFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)

        // Register the receiver dynamically.
        // For Android O (API 26) and above, you must specify if the receiver is exported.
        registerReceiver(phoneStateReceiver, intentFilter, RECEIVER_EXPORTED)
        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)

    }

    private fun listenForPermissionEvents() {
        applicationScope.launch {
            notificationPermissionEventBus.events().collect { event ->
                when (event) {
                    is NotificationPermissionEvent.PermissionGranted -> {
                        Log.d(TAG, "Notification permission granted event received.")
                        // Retry starting foreground service if we are already connected
                        // We rely on the try-catch inside startForegroundService logic
                        // to handle if we are technically in the background (though usually
                        // granting a permission brings the app/service interaction to foreground).
                        attemptStartForeground()
                    }
                    is NotificationPermissionEvent.PermissionDenied -> {
                        Log.w(TAG, "Notification permission denied event received. Service may run with reduced priority.")
                    }
                    else -> { /* Ignore others */ }
                }
            }
        }
    }


    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected. Attempting to start Foreground Service.")
        attemptStartForeground()
    }

    // --- REFACTORED: Moved logic to helper method to be called from onListenerConnected OR EventBus ---
    private fun attemptStartForeground() {
        val notification = createForegroundServiceNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Attempt to promote to Foreground
                startForeground(
                    1,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
                Log.d(TAG, "Foreground service started successfully!")
            } catch (e: Exception) {
                // If the system blocks us (AppOps error), we catch it here.
                // The service is NOT dead; it just continues as a standard background NotificationListener.
                // This typically happens only during development installs/updates.
                Log.w(TAG, "System blocked Foreground Service start (Background Restrictions). Running as standard listener. Error: ${e.message}")
            }
            return
        }
        try {
            startForeground(1, notification)
            Log.d(TAG, "Foreground service started the old fashioned way!")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start legacy foreground service: ${e.message}")
        }
    }

    private fun createForegroundServiceNotification(): Notification {
        // Create a minimal notification for the foreground service.
        // This notification is visible to the user and indicates your app is active.
        val channelId = "SpeakifyForegroundServiceChannel"
        val channelName = "Speakify Background Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Speakify Active")
            .setContentText("Processing notifications in the background.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startListeningForNotifications() {
        applicationScope.launch {
            settingsRepository.selectedTTSVoice.collect { selectedTTSVoice ->
                // Logic to process app settings and maybe update TTS instances
                defaultVoice = selectedTTSVoice ?: Constants.DefaultTTSVoice

                // This collector will run continuously in the background.
                Log.d(TAG, "Ready to process notifications.")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null)
            return

        // if we somehow got a notification about our app, we're done!
        if (sbn.packageName == "com.mikewarren.speakify") {
            return
        }

        if (Clerk.user == null) {
            return
        }

        // Launch a new coroutine for each notification.
        // This is non-blocking and uses the robust application scope.
        applicationScope.launch {
            processNotification(sbn)
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {

        val importantApps = userAppsDao.getAll()

        if (!importantApps.map { model -> model.packageName }.contains(sbn.packageName))
            return

        // we're passing responsibility for this to PhoneStateReceiver
        if (PackageNames.PhoneAppList.contains(sbn.packageName))
            return

        val lastSpokenTime = recentlySpokenCache.get(sbn.key)
        val currentTime = System.currentTimeMillis()
        if (lastSpokenTime != null && (currentTime - lastSpokenTime) < DEBOUNCE_TIME_MS) {
            Log.d(TAG, "Notification ${sbn.key} was spoken recently. Debouncing.")
            return
        }

        // construct a model for reading the notification
        // if there are no app settings, we should assume that every notification from the app in question...is important...and worth speaking!
        var appSettingsModel = AppSettingsModel.FromDbModel(appSettingsDao.getByPackageName(sbn.packageName))
        if (appSettingsModel == null) {
            appSettingsModel = AppSettingsModel(sbn.packageName, defaultVoice)
        }

        // build the notification strategy for this app
        val notificationStrategy = NotificationStrategyFactory.CreateFrom(sbn, appSettingsModel, settingsRepository.getContext(), ttsManager)
        notificationStrategy.logNotification()
        if (notificationStrategy.shouldSpeakify()) {
            recentlySpokenCache.put(sbn.key, currentTime)
            notificationStrategy.speakify()
        }

    }

    override fun onDestroy() {
        ttsManager.shutdown()

        Log.d(TAG, "Service destroyed. Unregistering PhoneStateReceiver.")

        // IMPORTANT: Always unregister the receiver to avoid memory leaks.
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: Exception) {
            // Can throw an exception if the receiver was never registered, so catch it.
            LogUtils.LogNonFatalError(TAG, "Error unregistering PhoneStateReceiver", e)
        }

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            LogUtils.LogNonFatalError(TAG, "Error unregistering ScreenStateReceiver", e)
        }

        super.onDestroy()
    }

}