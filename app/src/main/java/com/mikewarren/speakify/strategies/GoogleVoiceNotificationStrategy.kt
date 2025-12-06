package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.strategies.SMSNotificationStrategy.SMSNotificationType
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils

class GoogleVoiceNotificationStrategy(notification: StatusBarNotification,
                                      appSettingsModel: AppSettingsModel?,
                                      context: Context,
                                      ttsManager: TTSManager): BasePhoneNotificationStrategy(notification, appSettingsModel, context, ttsManager),
IMessageNotificationHandler<GoogleVoiceNotificationStrategy.NotificationType> {

    enum class NotificationType(val stringValue: String) {
        IncomingSMS("incoming sms"),
        OutgoingSMS("outgoing sms"),
        IncomingCall("incoming call"),
        OutgoingCall("outgoing call"),
        MissedCall("missed call"),
        Other("other"),
    }

    override fun getNotificationType() : NotificationType {
        val baseNotificationType = super.getNotificationType()
        if ((baseNotificationType != getOtherType()) ||
            (notification.notification.actions.isNullOrEmpty()))
            return baseNotificationType


        val actionTitlesLowercased = this.getActionTitlesLowercased()
        if (actionTitlesLowercased.contains("answer"))
            return NotificationType.IncomingCall
        if (SearchUtils.HasAnyOverlap(listOf("end call", "hang up", "speaker"), actionTitlesLowercased))
            return NotificationType.OutgoingCall
        if (actionTitlesLowercased.contains("call back"))
            return NotificationType.MissedCall

        return NotificationType.Other
    }

    override fun isFromSentMessage(): Boolean {
        return getMessages().isNotEmpty() && super.isFromSentMessage()
    }

    override fun getOutgoingSMSType(): NotificationType {
        return NotificationType.OutgoingSMS
    }

    override fun getIncomingSMSType(): NotificationType {
        return NotificationType.IncomingSMS
    }

    override fun getOtherType(): NotificationType {
        return NotificationType.Other
    }

    override fun getPossiblePersonExtras(): Array<String> {
        return arrayOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_MESSAGING_PERSON,
        )
    }

    override fun extractContactModel(): ContactModel {
        return NotificationExtractionUtils.ExtractContactModel(context,
            notification,
            getPossiblePersonExtras(),
            { sbn: StatusBarNotification, extrasKey: String ->
                if (getNotificationType() == NotificationType.IncomingSMS) {
                    if ((extrasKey == Notification.EXTRA_CONVERSATION_TITLE) &&
                        (sbn.notification.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)))
                        return@ExtractContactModel false
                }

                return@ExtractContactModel true
            })
    }

    override fun textToSpeakify(): String {
        val contactModel = extractContactModel()
        var suffix = ""
        if (contactModel.name.isNotEmpty())
            suffix = " from ${contactModel.name}"

        val notificationType = getNotificationType()
        if (notificationType == NotificationType.IncomingSMS)
            return "Incoming Google Voice Text Message${suffix}"
        if (notificationType == NotificationType.IncomingCall)
            return "Incoming Google Voice Call${suffix}"

        // TODO: should we report the notification back to the dev team in this instance ?
        return "Unknown Google Voice Notification"
    }

    override fun shouldSpeakify(): Boolean {
        val notificationType = getNotificationType()
        if (listOf(NotificationType.OutgoingCall, NotificationType.OutgoingSMS)
                .contains(notificationType))
            return false

        val baseShouldSpeakify = super.shouldSpeakify()
        if ((baseShouldSpeakify) && (notificationType == NotificationType.Other)) {
            logNotification()
            doLog("notificationType == ${notificationType}")
            throw IllegalStateException("Notification type is unknown, but we are trying to speakify it?!")
        }

        return baseShouldSpeakify
    }
}