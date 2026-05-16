package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils

class SMSNotificationStrategy(notification: StatusBarNotification,
                              appSettingsModel: AppSettingsModel?,
                              context: Context,
                              ttsManager: TTSManager,
) : BasePhoneNotificationStrategy(notification, appSettingsModel, context, ttsManager),
IOSReactionHandler<SMSNotificationStrategy.SMSNotificationType>,
ITaggable {


    enum class SMSNotificationType(val stringValue: String) {
        IncomingSMS("incoming sms"),
        OutgoingSMS("outgoing sms"),
        Other("other"),
    }

    override fun getNotificationType() : SMSNotificationType {
        val baseNotificationType = super.getNotificationType()
        if ((baseNotificationType != getOtherType()) ||
                (notification.notification.actions.isNullOrEmpty()))
            return baseNotificationType

        // --- Fallback or further checks if actions aren't definitive ---
        // If it's identified as MessagingStyle and has messages, that's also very strong.
        val messagingStyle = getMessagingStyle()
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            // If we reached here, it means the explicit action check above might not have passed,
            // but it IS a messaging style notification with messages.
            // We already filtered for "Self" in the extractedContactModel check.
            return SMSNotificationType.IncomingSMS
        }

        LogUtils.LogWarning(TAG, "Notification does not appear to be a standard speakable incoming message based on actions or MessagingStyle content.")

        return SMSNotificationType.Other
    }

    override fun getPossiblePersonExtras(): Array<String> {
        return arrayOf(
            Notification.EXTRA_CONVERSATION_TITLE,
            IMessageNotificationHandler.EXTRA_IM_PARTICIPANT_NORMALIZED_DESTINATION,
        )
    }

    override fun shouldSpeakify(): Boolean {
        if (!super.shouldSpeakify())
            return false

        if (extractedContactModel == ContactModel()) {
            return false
        }

        return (getNotificationType() == SMSNotificationType.IncomingSMS) &&
                (extractedContactModel.name != IMessageNotificationHandler.SelfName) &&
                (shouldSpeakifyBasedOnSettings())
    }

    override fun textToSpeakify(): String {
        if (isReadMessagesEnabled) {
            var contactName =  extractedContactModel.name
            if (contactName.isEmpty())
                contactName = context.getString(R.string.contact_unknown)

            return context.getString(R.string.sms_text_out_loud,
                contactName,
                NotificationExtractionUtils.ExtractText(notification),
            )
        }

        if (extractedContactModel.name.isEmpty())
            return context.getString(R.string.sms_notification_strategy_text,
                extractedContactModel.phoneNumber)

        return context.getString(R.string.sms_notification_strategy_text,
            extractedContactModel.name)
    }

    override fun extractContactModel(): ContactModel {
        // there may be SMS notifications from which we CANNOT extract messages
        if (getMessages().size <= 1) {
            val simplyExtractedContactModel = extractSimpleContactModel()
            if (simplyExtractedContactModel != null)
                return simplyExtractedContactModel
        }

        val notificationExtras = notification.notification.extras

        val notificationTitleExtra = notificationExtras.getString(Notification.EXTRA_TITLE)
        if ((notificationTitleExtra.isNullOrEmpty()) || (!notificationTitleExtra.contains("MessagingStyle")))
            return ContactModel()

        val latestMessage = getLatestMessage()

        if (latestMessage == null) {
            return ContactModel()
        }

        val senderPerson = latestMessage.person

        if (senderPerson == null) {
            logNotification()
            throw IllegalStateException("Somehow we got the notification, and messages, but no person was found.")
        }

        var name = senderPerson.name?.toString() ?: ""
        if ((!senderPerson.uri.isNullOrEmpty()) && (getMessagingStyle()!!.user.uri == senderPerson.uri))
            name = IMessageNotificationHandler.SelfName

        var phoneNumber = ""
        senderPerson.uri?.let { uriString ->
            if (uriString.startsWith("tel:")) {
                phoneNumber = uriString.substringAfter("tel:")
            }
        }

        return ContactModel(name, phoneNumber)
    }

    private fun extractSimpleContactModel(): ContactModel? {
        val simplyExtractedContactModel = NotificationExtractionUtils.ExtractContactModel(
            context,
            notification,
            this.getPossiblePersonExtras(),
            { sbn: StatusBarNotification, extrasKey: String ->
                if ((extrasKey == Notification.EXTRA_CONVERSATION_TITLE) &&
                    (sbn.notification.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION))
                )
                    return@ExtractContactModel false

                return@ExtractContactModel true
            },
        )

        if ((simplyExtractedContactModel.name.isNotEmpty()) ||
            (simplyExtractedContactModel.phoneNumber.isNotEmpty())
        )
            return simplyExtractedContactModel

        return null
    }

    override fun getOutgoingMessageType(): SMSNotificationType {
        return SMSNotificationType.OutgoingSMS
    }

    override fun getIncomingMessageType(): SMSNotificationType {
        return SMSNotificationType.IncomingSMS
    }

    override fun getOtherType(): SMSNotificationType {
        return SMSNotificationType.Other
    }

    override fun isReaction(): Boolean {
        if (super.isReaction())
            return true

        val notificationText = NotificationExtractionUtils.ExtractText(notification)
        val firstEmojiPosition = SearchUtils.GetEmojiPosition(notificationText)

        if (firstEmojiPosition == -1)
            return false

        val firstEmoji = notificationText[firstEmojiPosition]

        return notificationText.substring(firstEmojiPosition)
            .contains(context.getString(R.string.emoji_to_your_message,
                firstEmoji))
    }

    override fun logNotification() {
        super.logNotification()
        val messages = getMessages()
        if (messages.isNotEmpty()) {
            doLog("--- MessagingStyle Messages ---")
            messages.forEachIndexed { index, message ->
                doLog("  Message [$index]:")
                doLog("    Timestamp: ${message.timestamp}")
                doLog("    Text: ${message.text}")
                val person = message.person
                if (person != null) {
                    doLog("    Person Name: ${person.name}")
                    doLog("    Person URI: ${person.uri}")
                } else {
                    doLog("    Person: null")
                }
            }
        }
    }


}