package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.mikewarren.speakify.data.AppSettingsModel

abstract class BaseNotificationStrategy(
    val notification: StatusBarNotification,
    val appSettings: AppSettingsModel?,
    val context: Context,
    val tts: TextToSpeech?,
) {

    fun speakify() {
        tts?.speak(textToSpeakify(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    abstract fun textToSpeakify() : String
    open fun shouldSpeakify() : Boolean {
        // if the app settings is null or notification sources is empty, we should speakify
        return (appSettings == null || appSettings.notificationSources.isEmpty())
    }

}