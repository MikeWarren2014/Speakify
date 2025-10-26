package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.utils.AppNameHelper

object NotificationStrategyFactory {
    fun CreateFrom(notification: StatusBarNotification,
                   appSettings: AppSettingsModel?,
                   context: Context,
                   tts: TextToSpeech?,
    ) : BaseNotificationStrategy {
        if (Constants.MessagingAppPackageNames.contains(notification.packageName))
            return SMSNotificationStrategy(notification, appSettings, context, tts)

        return SimpleNotificationStrategy(notification, appSettings, context, tts)
    }
}