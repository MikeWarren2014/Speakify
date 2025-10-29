package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.services.TTSManager

object NotificationStrategyFactory {
    fun CreateFrom(notification: StatusBarNotification,
                   appSettingsModel: AppSettingsModel?,
                   context: Context,
                   ttsManager: TTSManager,
    ) : BaseNotificationStrategy {
        if (PackageNames.MessagingAppList.contains(notification.packageName))
            return SMSNotificationStrategy(notification, appSettingsModel, context, ttsManager)

        if (notification.packageName == PackageNames.GoogleVoice)
            return GoogleVoiceNotificationStrategy(notification, appSettingsModel, context, ttsManager)

        if (notification.packageName == PackageNames.GoogleCalendar)
            return GoogleCalendarNotificationStrategy(notification, appSettingsModel, context, ttsManager)

        return SimpleNotificationStrategy(notification, appSettingsModel, context, ttsManager)
    }
}