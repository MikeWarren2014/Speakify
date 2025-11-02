package com.mikewarren.speakify.strategies

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.mikewarren.speakify.utils.SearchUtils

interface IMessageNotificationHandler {

    companion object {
        val SelfName = "Self"
    }

    val notification: StatusBarNotification

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

    fun getLastSenderPerson(): Person? {
        return getMessages()
            .last()
            .person
    }

    fun getMessages(): List<NotificationCompat.MessagingStyle.Message> {
        val messagingStyle = getMessagingStyle()

        if (messagingStyle != null)
            return messagingStyle.messages

        Log.w("SMSNotificationStrategy", "Could not extract MessagingStyle, though EXTRA_MESSAGES might be present.")
        if (notification.notification.extras.containsKey(Notification.EXTRA_MESSAGES))
            Log.d("SMSNotificationStrategy", "EXTRA_MESSAGES is present.")
        return emptyList()
    }

    fun getMessagingStyle() : NotificationCompat.MessagingStyle? {
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
    }
}