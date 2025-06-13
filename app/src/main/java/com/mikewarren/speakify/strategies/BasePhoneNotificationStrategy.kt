package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel

abstract class BasePhoneNotificationStrategy(notification: StatusBarNotification,
                                    appSettings: AppSettingsModel?,
                                    context: Context,
                                    tts: TextToSpeech?) : BaseNotificationStrategy(notification, appSettings, context, tts) {

    protected val extractedContactModel = this.extractContactModel()

    protected abstract fun getPossiblePersonExtras() : Array<String>

    protected abstract fun extractContactModel(): ContactModel

    fun getActionTitlesLowercased(): List<String> {
        val actions = notification.notification.actions
        if (actions == null)
            return emptyList()

        return actions.map { action: Notification.Action ->  action.title.toString().lowercase() }
    }

    override fun shouldSpeakify(): Boolean {
        // TODO: should we check phone notification types here?

        if (super.shouldSpeakify())
            return true;

        // get the phone number from the status bar notification, and compare it against the app settings notification sources
        return appSettings?.notificationSources?.contains(extractedContactModel.phoneNumber) ?: false
    }

}