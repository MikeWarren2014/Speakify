package com.mikewarren.speakify.strategies

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
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


    fun getMessagingStyle() : NotificationCompat.MessagingStyle? {
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
    }
}