package com.mikewarren.speakify.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class YourNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

//        sbn.notification.
        throw NotImplementedError()
    }
}