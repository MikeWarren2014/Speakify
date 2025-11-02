package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils

class SMSNotificationStrategy(notification: StatusBarNotification,
                              appSettingsModel: AppSettingsModel?,
                              context: Context,
                              ttsManager: TTSManager,
) : BasePhoneNotificationStrategy(notification, appSettingsModel, context, ttsManager),
IMessageNotificationHandler {


    enum class SMSNotificationType(val stringValue: String) {
        IncomingSMS("incoming sms"),
        OutgoingSMS("outgoing sms"),
        Other("other"),
    }

    fun getSMSNotificationType() : SMSNotificationType {
        if (isFromSentMessage())
            return SMSNotificationType.OutgoingSMS

        val actions = notification.notification.actions
        if (actions == null)
            return SMSNotificationType.Other

        for (action in actions) {
            if (isReplyAction(action)) {
                // Check if it has RemoteInput for inline reply (stronger signal for actual reply)
                if (action.remoteInputs?.isNotEmpty() == true) {
                    Log.d(this.javaClass.name, "Notification has 'Reply' action. Likely an incoming message")
                    return SMSNotificationType.IncomingSMS
                }
            }
            if (isMarkAsReadAction(action)) {
                Log.d(this.javaClass.name, "Notification has 'Mark as read' action. Likely an incoming message")
                return SMSNotificationType.IncomingSMS
            }
        }

        // --- Fallback or further checks if actions aren't definitive ---
        // If it's identified as MessagingStyle and has messages, that's also very strong.
        val messagingStyle = getMessagingStyle()
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            // If we reached here, it means the explicit action check above might not have passed,
            // but it IS a messaging style notification with messages.
            // We already filtered for "Self" in the extractedContactModel check.
            Log.d(this.javaClass.name, "Is MessagingStyle with messages. Likely an incoming message.")
            return SMSNotificationType.IncomingSMS
        }

        Log.w(this.javaClass.name, "Notification does not appear to be a standard speakable incoming message based on actions or MessagingStyle content.")

        return SMSNotificationType.Other
    }

    override fun getPossiblePersonExtras(): Array<String> {
        return arrayOf(
            Notification.EXTRA_CONVERSATION_TITLE,

        )
    }

    override fun shouldSpeakify(): Boolean {
        if (!super.shouldSpeakify())
            return false

        if (extractedContactModel == ContactModel()) {
            Log.w(this.javaClass.name, "Could not extract contact model from notification.")
            return false
        }

        return (getSMSNotificationType() == SMSNotificationType.IncomingSMS) && (extractedContactModel.name != IMessageNotificationHandler.SelfName)
    }

    override fun textToSpeakify(): String {
        if (extractedContactModel.name.isEmpty())
            return "Text from ${extractedContactModel.phoneNumber}"

        return "Text from ${extractedContactModel.name}"
    }

    override fun extractContactModel(): ContactModel {
        val simplyExtractedContactModel = NotificationExtractionUtils.ExtractContactModel(context,
            notification,
            this.getPossiblePersonExtras(),
            { sbn: StatusBarNotification, extrasKey: String ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if ((extrasKey == Notification.EXTRA_CONVERSATION_TITLE) &&
                        (sbn.notification.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)))
                        return@ExtractContactModel false
                }

                return@ExtractContactModel true
            },
        )

        if ((simplyExtractedContactModel.name.isNotEmpty()) ||
            (simplyExtractedContactModel.phoneNumber.isNotEmpty()))
            return simplyExtractedContactModel

        val notificationExtras = notification.notification.extras

        val notificationTitleExtra = notificationExtras.getString(Notification.EXTRA_TITLE)
        if ((notificationTitleExtra.isNullOrEmpty()) || (notificationTitleExtra.contains("MessagingStyle")))
            return simplyExtractedContactModel

        val allMessages = getMessages()

        if (allMessages.isEmpty()) {
            Log.d("SMSNotificationStrategy", "No messages found in MessagingStyle.")
            return simplyExtractedContactModel // Or handle as appropriate
        }

        // Now you can work with the 'allMessages' list
        val latestMessage = allMessages.last()
        val senderPerson = latestMessage.person

        // Now you can use senderPerson.name, senderPerson.uri etc.
        // to populate your ContactModel
        if (senderPerson != null) {
            var name = senderPerson.name?.toString() ?: ""
            if ((!senderPerson.uri.isNullOrEmpty()) && (getMessagingStyle()!!.user.uri == senderPerson.uri))
                name = IMessageNotificationHandler.SelfName

            var phoneNumber = ""
            senderPerson.uri?.let { uriString ->
                if (uriString.startsWith("tel:")) {
                    phoneNumber = uriString.substringAfter("tel:")
                }
                // You could also check for "mailto:" if relevant
            }

            return ContactModel(name, phoneNumber)
        }
        throw IllegalStateException("Somehow we got the notification, and messages, but no person was found.")
    }



}