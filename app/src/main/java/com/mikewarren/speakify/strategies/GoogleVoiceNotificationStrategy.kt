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
IMessageNotificationHandler {

    enum class NotificationType(val stringValue: String) {
        IncomingSMS("incoming sms"),
        OutgoingSMS("outgoing sms"),
        IncomingCall("incoming call"),
        OutgoingCall("outgoing call"),
        MissedCall("missed call"),
        Other("other"),
    }

    fun getNotificationType() : NotificationType {
        val actions = notification.notification.actions
        if (actions == null)
            return NotificationType.Other

        val messagingStyle = getMessagingStyle()
        if (messagingStyle != null) {
            for (action in actions) {
                if (isReplyAction(action)) {
                    // Check if it has RemoteInput for inline reply (stronger signal for actual reply)
                    if (!action.remoteInputs.isNullOrEmpty()) {
                        Log.d(this.javaClass.name, "Notification has 'Reply' action. Likely an incoming message")
                        return NotificationType.IncomingSMS
                    }
                }
                if (isMarkAsReadAction(action)) {
                    Log.d(this.javaClass.name, "Notification has 'Mark as read' action. Likely an incoming message")
                    return NotificationType.IncomingSMS
                }
            }

            if (messagingStyle.messages.isNotEmpty()) {
                TODO("get the last message and make sure that it wasn't sent from Self")
            }

            return NotificationType.IncomingSMS
        }

        val actionTitlesLowercased = this.getActionTitlesLowercased()
        if (actionTitlesLowercased.contains("answer"))
            return NotificationType.IncomingCall
        if (SearchUtils.HasAnyOverlap(listOf("end call", "hang up", "speaker"), actionTitlesLowercased))
            return NotificationType.OutgoingCall
        if (actionTitlesLowercased.contains("call back"))
            return NotificationType.MissedCall

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

        return super.shouldSpeakify()
    }
}