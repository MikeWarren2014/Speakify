package com.mikewarren.speakify.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.AppSettingsWithNotificationSources
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.strategies.NotificationStrategyFactory
import com.mikewarren.speakify.utils.TTSUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
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

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob) // Use IO dispatcher for DB operations

    // TODO: we need a cache of TTS instances...that will get destroyed and overwritten if their voice name is different from that which is saved on the appSettingsModel...
    private val packageTTSDict = mutableMapOf<String, TextToSpeech>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null)
            return

        // if we somehow got a notification about our app, we're done!
        if (sbn?.packageName == "com.mikewarren.speakify") {
            return
        }

        serviceScope.launch {
            settingsRepository.selectedTTSVoice.collectLatest { ttsVoice: String? ->
                val defaultVoice = ttsVoice ?: Constants.DefaultTTSVoice

                val importantApps = userAppsDao.getAll()

                if (!importantApps.map { model -> model.packageName }.contains(sbn?.packageName))
                    return@collectLatest

                // construct a model for reading the notification
                // if there are no app settings, we should assume that every notification from the app in question...is important...and worth speaking!
                val allAppSettings = appSettingsDao.getAll()
                    .map { dbModel : AppSettingsWithNotificationSources -> AppSettingsModel.FromDbModel(dbModel) }

                var appSettingsModel = allAppSettings.find { model: AppSettingsModel -> model.packageName == sbn?.packageName }
                if (appSettingsModel == null) {
                    appSettingsModel = AppSettingsModel(sbn.packageName, defaultVoice)
                }

                val tts = createOrUpdateTTS(appSettingsModel)

                // build the notification strategy for this app
                val notificationStrategy = NotificationStrategyFactory.CreateFrom(sbn, appSettingsModel, settingsRepository.getContext(), tts)
                if (notificationStrategy.shouldSpeakify()) {
                    notificationStrategy.textToSpeakify()
                }
            }

        }
    }

    fun createOrUpdateTTS(appSettingsModel: AppSettingsModel): TextToSpeech? {
        if (packageTTSDict.containsKey(appSettingsModel.packageName)) {
            val cachedTTS = packageTTSDict[appSettingsModel.packageName]

            if (cachedTTS == null)
                throw IllegalStateException("Somehow we have an entry on the packageTTSDict for package ${appSettingsModel.packageName}, but it's null!")

            if ((cachedTTS.voice.name != appSettingsModel.announcerVoice)) {
                TTSUtils.SetTTSVoice(cachedTTS, appSettingsModel.announcerVoice)
            }

            return cachedTTS
        }

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

        // Cancel all coroutines when the service is destroyed
        serviceJob.cancel()
    }
}