package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager

class GoogleCalendarNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager
) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {

    override fun shouldSpeakify(): Boolean {
        // First, basic checks from the base class
        if (!super.shouldSpeakify()) return false

        // Log all extras to help with development
        notification.notification.extras.keySet().forEach { key ->
            Log.d("CalendarStrategy", "Extra: $key = ${notification.notification.extras.get(key)}")
        }

        // We only care about calendar events, not other notifications it might post
        // (You might need to refine this by inspecting the logged extras)
        val title = notification.notification.extras.getString(Notification.EXTRA_TITLE)
        return !title.isNullOrEmpty()
    }


    override fun textToSpeakify(): String {
        val extras = notification.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        return context.getString(R.string.notification_calendar_upcoming, title, text)
    }
}
