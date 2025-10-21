package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.utils.NotificationExtractionUtils

class PhoneNotificationStrategy(notification: StatusBarNotification,
    appSettings: AppSettingsModel?,
    context: Context,
    tts: TextToSpeech?) : BasePhoneNotificationStrategy(notification, appSettings, context, tts) {



    enum class PhoneNotificationType(val stringValue: String) {
        IncomingCall("incoming call"),
        MissedCall("missed call"),
        OutgoingCall("call in progress"),
        Other("other"),
    }

    fun getPhoneNotificationType() : PhoneNotificationType {
        // get the actions from this notification
        val actions = notification.notification.actions

        if (actions == null)
            return PhoneNotificationType.Other

        val actionTitlesLowercased = this.getActionTitlesLowercased()
        if (actionTitlesLowercased.contains("answer"))
            return PhoneNotificationType.IncomingCall
        if (listOf("end call", "hang up", "speaker").intersect(actionTitlesLowercased).isNotEmpty())
            return PhoneNotificationType.OutgoingCall
        if (actionTitlesLowercased.contains("call back"))
            return PhoneNotificationType.MissedCall

        return PhoneNotificationType.Other
    }

    override fun getPossiblePersonExtras(): Array<String> {
        return arrayOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_TITLE,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_BIG_TEXT,
        )
    }

    override fun extractContactModel(): ContactModel {
        return NotificationExtractionUtils.ExtractContactModel(context,
            notification,
            this.getPossiblePersonExtras(),
        )
    }

    override fun textToSpeakify(): String {
        return "${getPhoneNotificationType().stringValue} from ${extractedContactModel.name}"
    }

    override fun shouldSpeakify(): Boolean {
        val notificationType = getPhoneNotificationType()
        if (notificationType == PhoneNotificationType.OutgoingCall)
            return false

       return super.shouldSpeakify()
    }
}