package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.utils.AppNameHelper

class SimpleNotificationStrategy(notification: StatusBarNotification,
                                 appSettings: AppSettingsModel?,
                                 context: Context) : BaseNotificationStrategy(notification, appSettings, context) {
    override fun textToSpeakify(): String {
        return "Notification from ${AppNameHelper(context).getAppDisplayName(notification.packageName)}"
    }
}