package com.mikewarren.speakify.services

import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import android.util.Log
import android.util.LruCache
import com.clerk.api.Clerk
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.receivers.PhoneStateReceiver
import com.mikewarren.speakify.strategies.NotificationStrategyFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpeakifyNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userAppsDao: UserAppsDao

    @Inject
    lateinit var appSettingsDao: AppSettingsDao

    @Inject
    lateinit var notificationSourcesDao: NotificationSourcesDao


    // Inject the application-level CoroutineScope
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

        Log.d("SpeakifyNLS", "Service created. Registering PhoneStateReceiver.")

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

    private fun startListeningForNotifications() {
        applicationScope.launch {
            settingsRepository.selectedTTSVoice.collect { selectedTTSVoice ->
                // Logic to process app settings and maybe update TTS instances
                defaultVoice = selectedTTSVoice ?: Constants.DefaultTTSVoice

                // This collector will run continuously in the background.
                Log.d("SpeakifyNLS", "Ready to process notifications.")
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
            Log.w("SpeakifyNLS", "onNotificationPosted: Clerk.user is null. Not reading the notification")
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
            Log.d("SpeakifyNLS", "Notification ${sbn.key} was spoken recently. Debouncing.")
            return // Ignore this notification
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
        super.onDestroy()

        ttsManager.shutdown()

        Log.d("SpeakifyNLS", "Service destroyed. Unregistering PhoneStateReceiver.")

        // IMPORTANT: Always unregister the receiver to avoid memory leaks.
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: Exception) {
            // Can throw an exception if the receiver was never registered, so catch it.
            Log.e("SpeakifyNLS", "Error unregistering PhoneStateReceiver", e)
        }

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            Log.e("SpeakifyNLS", "Error unregistering ScreenStateReceiver", e)
        }
    }
}