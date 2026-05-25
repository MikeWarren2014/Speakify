package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessengerNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,
) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager),
    IThirdPartyMessageNotificationHandler<MessengerNotificationStrategy.MessengerNotificationTypes>,
    ITaggable {

    enum class MessengerNotificationTypes {
        IncomingMessage,
        OutgoingMessage,
        IncomingAudioCall,
        OutgoingCall,
        IncomingVideoCall,
        Other,
    }

    override val debounceTimeMillis: Long
        get() {
            val type = getNotificationType()
            return if (type == MessengerNotificationTypes.IncomingAudioCall ||
                type == MessengerNotificationTypes.IncomingVideoCall
            ) {
                0L // Don't debounce incoming calls
            } else {
                super.debounceTimeMillis
            }
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

    val text = NotificationExtractionUtils.ExtractText(notification)
    val title = NotificationExtractionUtils.ExtractTitle(notification)

    override fun isReaction(): Boolean {
        val prefixes = context.resources.getStringArray(R.array.facebook_reaction_prefixes)
        val suffix = context.getString(R.string.facebook_reaction_suffix)

        val textToCheck = if (text.contains(':')) text.substringBefore(':') else text

        val hasPrefix = prefixes.any { textToCheck.contains(it, ignoreCase = true) }
        val hasSuffix = textToCheck.contains(suffix, ignoreCase = true)

        return hasPrefix && hasSuffix && textToCheck.length > (suffix.length + 5)
    }

    private val senderName: String? by lazy {
        extractSenderName()
    }

    override fun getNotificationType(): MessengerNotificationTypes {
        val baseNotificationType = super.getNotificationType()
        if ((baseNotificationType == getIncomingMessageType()) || (baseNotificationType == getOutgoingMessageType())) {
            return baseNotificationType
        }

        val actions = notification.notification.actions ?: return getOtherType()

        Log.d(TAG, "baseNotificationType == ${baseNotificationType}")

        val actionTitles = actions.mapNotNull { it.title }

        if (SearchUtils.HasAnyOverlap(context.resources.getStringArray(R.array.action_outgoing_call),
            actionTitles)) {
            return MessengerNotificationTypes.OutgoingCall
        }
        if (SearchUtils.HasAnyMatchesOf(context.resources.getStringArray(R.array.action_incoming_call_list),
            actionTitles)) {
            if (isAudioCall())
                return MessengerNotificationTypes.IncomingAudioCall
            if (isVideoCall())
                return MessengerNotificationTypes.IncomingVideoCall
        }

        return getOtherType()
    }

    private fun isAudioCall() : Boolean {
        return text
            .contains(context.getString(R.string.messenger_incoming_audio_call_text), ignoreCase = true)
    }

    private fun isVideoCall() : Boolean {
        return text
            .contains(
                context.getString(R.string.messenger_incoming_video_call_text),
                ignoreCase = true
            )
    }

    override fun textToSpeakify(): String {
        val notificationType = getNotificationType()

        val notificationSource = senderName ?: context.getString(R.string.contact_unknown)

        if (notificationType == MessengerNotificationTypes.IncomingMessage) {
            if (isReadMessagesEnabled)
                return context.getString(R.string.messenger_text_out_loud,
                    notificationSource,
                    text,
                )
            return context.getString(R.string.messenger_notification_text,
                notificationSource)
        }
        if (notificationType == MessengerNotificationTypes.IncomingAudioCall)
            return context.getString(R.string.messenger_notification_audio_call,
                notificationSource)
        if (notificationType == MessengerNotificationTypes.IncomingVideoCall)
            return context.getString(R.string.messenger_notification_video_call,
                notificationSource)

        return context.getString(R.string.messenger_notification_strategy_unknown)
    }

    override fun shouldSpeakify(): Boolean {
        val name = senderName ?: return false

        // Save to recent contacts regardless of whether we speak it
        if (isPotentialContact(name))
            saveToRecentContacts(name)

        val notificationType = getNotificationType()
        doLog("notificationType == ${notificationType}")
        if (notificationType == MessengerNotificationTypes.Other) {
            doLog("Unknown Notification Type")
            logNotification()
            return false
        }

        if ((notificationType == MessengerNotificationTypes.OutgoingMessage) ||
            (notificationType == MessengerNotificationTypes.OutgoingCall)) {
            return false
        }

        return ((super.shouldSpeakify()) || (appSettingsModel!!.notificationSources.any { it.value == name })) &&
                (super.shouldSpeakifyBasedOnSettings())
    }

    private fun extractSenderName(): String? {
        val extras = notification.notification.extras
        
        // 1. Try MessagingStyle (Modern Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val messagingStyle = getMessagingStyle()
            if (messagingStyle != null) {
                // For non-group chats, the conversation title might be the name
                if (title.isNotEmpty() && !extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
                    return title
                }
                
                // Otherwise get the sender of the last message
                val person = getLatestSenderPerson()
                if (person != null) {
                    return person.name?.toString()
                }
            }
        }

        // 2. Fallback to EXTRA_TITLE
        return title
    }

    private fun isPotentialContact(name: String): Boolean {
        if (name.isBlank())
            return false

        if (name == "Messenger")
            return false

        if (SearchUtils.ContainsKeywords(context.resources.getStringArray(R.array.messenger_non_contact_titles),
            name))
            return false

        return true
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
