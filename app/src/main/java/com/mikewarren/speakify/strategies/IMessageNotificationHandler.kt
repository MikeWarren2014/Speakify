package com.mikewarren.speakify.strategies

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.mikewarren.speakify.strategies.SMSNotificationStrategy.SMSNotificationType
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.ITaggable

interface IMessageNotificationHandler<EnumType>: ITaggable {

    companion object {
        val SelfName = "Self"
    }

    val notification: StatusBarNotification

    fun getNotificationType(): EnumType {
        if (isFromSentMessage())
            return getOutgoingSMSType()

        val actions = notification.notification.actions
        if (actions == null)
            return getOtherType()

        for (action in actions) {
            if (isReplyAction(action)) {
                // Check if it has RemoteInput for inline reply (stronger signal for actual reply)
                if (action.remoteInputs?.isNotEmpty() == true) {
                    Log.d(this.javaClass.name, "Notification has 'Reply' action. Likely an incoming message")
                    return getIncomingSMSType()
                }
            }
            if (isMarkAsReadAction(action)) {
                Log.d(this.javaClass.name, "Notification has 'Mark as read' action. Likely an incoming message")
                return getIncomingSMSType()
            }
        }

        return getOtherType()
    }

    fun isReplyAction(action: Notification.Action): Boolean {
        return SearchUtils.HasAnyMatches(listOf("Reply",
            "Répondre",
            "Responder",
        ), action.title.toString())
    }

    fun isMarkAsReadAction(action: Notification.Action): Boolean {
        return SearchUtils.HasAnyMatches(listOf("Mark as read",
            "Mark Read",
            "Marquer comme lu",
            "Marquer lu",
            "Marcar como leído",
            "Marcar leído",
        ), action.title.toString())
    }

    fun isFromSentMessage(): Boolean {
        return getLastSenderPerson() == null
    }

    fun getOutgoingSMSType(): EnumType
    fun getIncomingSMSType(): EnumType
    fun getOtherType() : EnumType

    fun getLastSenderPerson(): Person? {
        return getMessages()
            .last()
            .person
    }

    fun getMessages(): List<NotificationCompat.MessagingStyle.Message> {
        val messagingStyle = getMessagingStyle()

        if (messagingStyle != null)
            return messagingStyle.messages

        Log.w(TAG, "Could not extract MessagingStyle, though EXTRA_MESSAGES might be present.")
        val extras = notification.notification.extras
        if (extras.containsKey(Notification.EXTRA_MESSAGES)) {
            Log.d(TAG, "MessagingStyle missing, but EXTRA_MESSAGES found. Attempting manual parse.")
            // TODO: move all this to NotificationExtractionUtils, or somewhere similar
            return NotificationExtractionUtils.ExtractMessagesManually(extras)
        }

        Log.w(TAG, "Could not extract messages via Style or Extras.")
        return emptyList()
    }

    fun getMessagingStyle() : NotificationCompat.MessagingStyle? {
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
    }
}