package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets.MessengerAdditionalSettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessengerNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,
) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager),
    IMessageNotificationHandler<MessengerNotificationStrategy.MessengerNotificationTypes>,
    ITaggable {

    enum class MessengerNotificationTypes {
        IncomingMessage,
        OutgoingMessage,
        IncomingCall,
        OutgoingCall,
        Other,
    }

    override fun getOutgoingMessageType(): MessengerNotificationTypes {
        return MessengerNotificationTypes.OutgoingMessage
    }

    override fun getIncomingMessageType(): MessengerNotificationTypes {
        return MessengerNotificationTypes.IncomingMessage
    }

    override fun getOtherType(): MessengerNotificationTypes {
        return MessengerNotificationTypes.Other
    }

    private val senderName: String? by lazy {
        extractSenderName()
    }

    override fun textToSpeakify(): String {
        val notificationType = getNotificationType()

        val notificationSource = senderName ?: context.getString(R.string.contact_unknown)

        if (notificationType == MessengerNotificationTypes.IncomingMessage)
            return context.getString(R.string.messenger_notification_text,
                notificationSource)
        if (notificationType == MessengerNotificationTypes.IncomingCall)
            return context.getString(R.string.messenger_notification_call,
                notificationSource)

        return context.getString(R.string.messenger_notification_strategy_unknown)
    }

    override fun shouldSpeakify(): Boolean {
        val name = senderName ?: return false

        // 1. Check "Ignore Message Requests" setting
        if (isMessageRequest() && 
            (appSettingsModel?.getBooleanSetting(MessengerAdditionalSettingsViewModel.KEY_IGNORE_MESSAGE_REQUESTS, true) == true)) {
            doLog("Ignoring notification as it appears to be a Message Request.")
            return false
        }
        
        // Save to recent contacts regardless of whether we speak it
        saveToRecentContacts(name)

        val notificationType = getNotificationType()
        if (notificationType == MessengerNotificationTypes.Other) {
            logNotification()
            doLog("notificationType == ${notificationType}")
            return false
        }

        if ((notificationType == MessengerNotificationTypes.OutgoingMessage) ||
            (notificationType == MessengerNotificationTypes.OutgoingCall)) {
            return false
        }

        return (super.shouldSpeakify()) ||
                (appSettingsModel!!.notificationSources.contains(name))
    }

    private fun isMessageRequest(): Boolean {
        val extras = notification.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val messageRequestKeywords = listOf("Message Request", "wants to connect", "requested to message")
        
        return messageRequestKeywords.any { 
            title.contains(it, ignoreCase = true) || text.contains(it, ignoreCase = true) 
        }
    }

    private fun extractSenderName(): String? {
        val extras = notification.notification.extras
        
        // 1. Try MessagingStyle (Modern Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val messagingStyle = getMessagingStyle()
            if (messagingStyle != null) {
                // For non-group chats, the conversation title might be the name
                val title = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
                if (!title.isNullOrEmpty() && !extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
                    return title
                }
                
                // Otherwise get the sender of the last message
                val person = getLastSenderPerson()
                if (person != null) {
                    return person.name?.toString()
                }
            }
        }

        // 2. Fallback to EXTRA_TITLE
        return extras.getString(Notification.EXTRA_TITLE)
    }

    private fun saveToRecentContacts(name: String) {
        // We use a separate scope to avoid blocking the notification processing
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DbProvider.GetDb(context)
                db.recentMessengerContactDao().insertContact(RecentMessengerContactModel(name))
            } catch (e: Exception) {
                // Log failure but don't crash
                LogUtils.LogNonFatalError(TAG,
                    "Error saving to recent contacts: ${e.message}",
                    e)
            }
        }
    }
}
