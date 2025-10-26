package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.services.TTSManager

object NotificationStrategyFactory {
    fun CreateFrom(notification: StatusBarNotification,
                   appSettingsModel: AppSettingsModel?,
                   context: Context,
                   ttsManager: TTSManager,
    ) : BaseNotificationStrategy {
        if (Constants.MessagingAppPackageNames.contains(notification.packageName))
            return SMSNotificationStrategy(notification, appSettingsModel, context, ttsManager)

        return SimpleNotificationStrategy(notification, appSettingsModel, context, ttsManager)
    }
}