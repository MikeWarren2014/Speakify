package com.mikewarren.speakify.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import com.mikewarren.speakify.ApplicationScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.strategies.NotificationStrategyFactory
import com.mikewarren.speakify.strategies.PhoneNotificationStrategy
import com.mikewarren.speakify.utils.TTSUtils
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

    private val packageTTSDict = mutableMapOf<String, TextToSpeech>()

    private lateinit var defaultVoice: String;

    override fun onCreate() {
        super.onCreate()
        // Start the collector when the service is created.
        // This ensures it's always listening as long as the service process is alive.
        startListeningForNotifications()
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

        // Launch a new coroutine for each notification.
        // This is non-blocking and uses the robust application scope.
        applicationScope.launch {
            processNotification(sbn)
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {

        val importantApps = userAppsDao.getAll()

        if ((sbn.packageName.contains("dialer")) || (sbn.packageName.contains("phone")))
            PhoneNotificationStrategy(sbn, null, settingsRepository.getContext(), null)
                .logNotification()

        if (!importantApps.map { model -> model.packageName }.contains(sbn?.packageName))
            return

        // construct a model for reading the notification
        // if there are no app settings, we should assume that every notification from the app in question...is important...and worth speaking!
        var appSettingsModel = AppSettingsModel.FromDbModel(appSettingsDao.getByPackageName(sbn.packageName))
        if (appSettingsModel == null) {
            appSettingsModel = AppSettingsModel(sbn.packageName, defaultVoice)
        }

        val tts = createOrUpdateTTS(appSettingsModel)

        // build the notification strategy for this app
        val notificationStrategy = NotificationStrategyFactory.CreateFrom(sbn, appSettingsModel, settingsRepository.getContext(), tts)
        notificationStrategy.logNotification()
        if (notificationStrategy.shouldSpeakify()) {
            notificationStrategy.speakify()
        }

    }

    fun createOrUpdateTTS(appSettingsModel: AppSettingsModel): TextToSpeech? {
        if (packageTTSDict.containsKey(appSettingsModel.packageName)) {
            val cachedTTS = packageTTSDict[appSettingsModel.packageName]

            if (cachedTTS == null)
                throw IllegalStateException("Somehow we have an entry on the packageTTSDict for package ${appSettingsModel.packageName}, but it's null!")

            if (cachedTTS.voice == null)
                return createTTS(appSettingsModel);

            if (cachedTTS.voice.name != appSettingsModel.announcerVoice) {
                TTSUtils.SetTTSVoice(cachedTTS, appSettingsModel.announcerVoice)
            }

            return cachedTTS
        }

        return createTTS(appSettingsModel);

    }

    fun createTTS(appSettingsModel: AppSettingsModel): TextToSpeech? {
        packageTTSDict[appSettingsModel.packageName] = TextToSpeech(this, { status ->
            if (status == TextToSpeech.SUCCESS) {
                TTSUtils.SetTTSVoice(packageTTSDict[appSettingsModel.packageName], appSettingsModel.announcerVoice)
                return@TextToSpeech
            }
            Log.e(this.javaClass.name, "TTS initialization failed with status: $status")
        })

        return packageTTSDict[appSettingsModel.packageName]
    }


    fun shutdownTTS(tts: TextToSpeech) {
        tts.stop()
        tts.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()

        packageTTSDict.keys.forEach { packageName: String ->
            val tts = packageTTSDict[packageName]
            if (tts != null)
                shutdownTTS(tts)
        }

        packageTTSDict.clear()
    }
}