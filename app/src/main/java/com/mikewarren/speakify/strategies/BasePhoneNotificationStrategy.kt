package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.SearchUtils

abstract class BasePhoneNotificationStrategy(notification: StatusBarNotification,
                                             appSettingsModel: AppSettingsModel?,
                                             context: Context,
                                             ttsManager: TTSManager) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {

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
        if (super.shouldSpeakify())
            return true;

        // get the phone number from the status bar notification, and compare it against the app settings notification sources
        return SearchUtils.IsInPhoneNumberList(appSettingsModel?.notificationSources!!, extractedContactModel.phoneNumber)
    }

}