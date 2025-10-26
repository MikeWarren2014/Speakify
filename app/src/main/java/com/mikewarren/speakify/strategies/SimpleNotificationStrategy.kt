package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.AppNameHelper

class SimpleNotificationStrategy(notification: StatusBarNotification,
                                 appSettingsModel: AppSettingsModel?,
                                 context: Context,
                                 ttsManager: TTSManager) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    override fun textToSpeakify(): String {
        return "Notification from ${AppNameHelper(context).getAppDisplayName(notification.packageName)}"
    }
}