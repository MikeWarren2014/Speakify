package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.utils.AppNameHelper

class SimpleNotificationStrategy(notification: StatusBarNotification,
                                 appSettings: AppSettingsModel?,
                                 context: Context,
                                tts: TextToSpeech?) : BaseNotificationStrategy(notification, appSettings, context, tts) {
    override fun textToSpeakify(): String {
        return "Notification from ${AppNameHelper(context).getAppDisplayName(notification.packageName)}"
    }
}