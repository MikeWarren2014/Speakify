package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.mikewarren.speakify.R
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils

interface IMessageNotificationHandler<EnumType>: ITaggable {

    companion object {
        val SelfName = "Self"
        val EXTRA_IM_PARTICIPANT_NORMALIZED_DESTINATION = "extra_im_notification_participant_normalized_destination"
    }

    val notification: StatusBarNotification

    val context: Context

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
                    return getIncomingSMSType()
                }
            }
            if (isMarkAsReadAction(action)) {
                return getIncomingSMSType()
            }
        }
        
        // Fallback: If we have a participant destination but no clear actions, assume incoming
        // This handles cases where actions might be hidden or different in "sensitive" mode
        val extras = notification.notification.extras
        if (extras.containsKey(EXTRA_IM_PARTICIPANT_NORMALIZED_DESTINATION)) {
            return getIncomingSMSType()
        }

        return getOtherType()
    }

    fun isReplyAction(action: Notification.Action): Boolean {
        return context.getString(R.string.action_reply)
            .contains(action.title.toString(), true)
    }

    fun isMarkAsReadAction(action: Notification.Action): Boolean {
        return SearchUtils.HasAnyMatches(context.resources.getStringArray(R.array.action_mark_read),
            action.title.toString())
    }

    fun isFromSentMessage(): Boolean {
        return getLastSenderPerson() == null
    }

    fun getOutgoingSMSType(): EnumType
    fun getIncomingSMSType(): EnumType
    fun getOtherType() : EnumType

    fun getLastSenderPerson(): Person? {
        val messages = getMessages()
        if (messages.isNotEmpty()) {
            return messages.last().person
        }

        // If messages are empty (e.g., hidden content), check for the participant extra
        val extras = notification.notification.extras
        val participantDestination = extras.getString(EXTRA_IM_PARTICIPANT_NORMALIZED_DESTINATION)
        if (!participantDestination.isNullOrEmpty()) {
            Log.d(TAG, "Content hidden, but found participant destination: $participantDestination")
            return Person.Builder()
                .setName(participantDestination) // Use the phone number as the name
                .setKey(participantDestination)
                .build()
        }

        return null
    }

    fun getMessages(): List<NotificationCompat.MessagingStyle.Message> {
        val messagingStyle = getMessagingStyle()

        if (messagingStyle != null)
            return messagingStyle.messages

        LogUtils.LogWarning(TAG, "Could not extract MessagingStyle, though EXTRA_MESSAGES might be present.")
        val extras = notification.notification.extras
        if (extras.containsKey(Notification.EXTRA_MESSAGES)) {
            Log.d(TAG, "MessagingStyle missing, but EXTRA_MESSAGES found. Attempting manual parse.")
            return NotificationExtractionUtils.ExtractMessagesManually(extras)
        }

        LogUtils.LogWarning(TAG, "Could not extract messages via Style or Extras.")
        return emptyList()
    }

    fun getMessagingStyle() : NotificationCompat.MessagingStyle? {
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
    }
}
