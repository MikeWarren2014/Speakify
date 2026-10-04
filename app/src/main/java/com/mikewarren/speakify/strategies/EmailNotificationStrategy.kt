package com.mikewarren.speakify.strategies

import android.app.Notification
import androidx.core.app.Person
import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.constants.appSettingsKeys.EmailAppKeys
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.RecentEmailContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.AppNameHelper
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@IsEmailApp
class EmailNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,
): BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager),
    IMessageNotificationHandler<EmailNotificationStrategy.NotificationTypes> {
    enum class NotificationTypes {
        MultipleNewMessages,
        NewMessage,
        Sent,
        Other,
    }

    override fun getNotificationType(): NotificationTypes {
        val baseNotificationType = super.getNotificationType()
        if (baseNotificationType != NotificationTypes.Other)
            return baseNotificationType

        // For now, we naively implement this logic using captured Gmail notification logs
        if (SearchUtils.MatchesFormatString(context.getString(R.string.email_multiple_new_messages),
            title) != null)
            return NotificationTypes.MultipleNewMessages

        return NotificationTypes.Other
    }

    override fun getOutgoingMessageType(): NotificationTypes {
        return NotificationTypes.Sent
    }

    override fun getIncomingMessageType(): NotificationTypes {
        return NotificationTypes.NewMessage
    }

    override fun getOtherType(): NotificationTypes {
        return NotificationTypes.Other
    }

    override fun isReaction(): Boolean {
        return false
    }


    val isAnnounceMultipleMessagesEnabled : Boolean = appSettingsModel?.getBooleanSetting(EmailAppKeys.KEY_ANNOUNCE_MULTIPLE_MESSAGE) == true
    val messageKeywords : List<String> = appSettingsModel?.getListSetting(EmailAppKeys.KEY_MESSAGE_KEYWORDS) ?: emptyList()

    override fun getLatestSenderPerson(): Person? {
        val basePerson = super.getLatestSenderPerson()
        if (basePerson != null)
            return basePerson

        // if the messages-based logic did not work, we have our work cut out for us.
        // we will have to look at things like the title, the people list, ..., to determine, and build the Person.
        val peopleList = NotificationExtractionUtils.ExtractPersonList(notification) ?: return null
        val sender = peopleList.first()
        return Person.Builder()
            .setName(title)
            .setUri(sender.uri)
            .setKey(sender.key)
            .build()
    }

    override fun textToSpeakify(): String {
        val notificationType = getNotificationType()
        if ((isAnnounceMultipleMessagesEnabled) && (notificationType == NotificationTypes.MultipleNewMessages))
            return context.getString(R.string.multiple_new_messages, title)

        if (notificationType == NotificationTypes.NewMessage) {
            val emailMessageRawText = NotificationExtractionUtils.ExtractStringExtra(Notification.EXTRA_BIG_TEXT, notification)
            val subject = emailMessageRawText.substringBefore('\n')
            if (isReadMessagesEnabled) {
                val rawBody = emailMessageRawText.substringAfter('\n')
                val messageText = NotificationExtractionUtils.StripEmailQuotedReply(rawBody, context)

                return context.getString(
                    R.string.whole_email_message,
                    title,
                    subject,
                    messageText
                )
            }
            return context.getString(
                R.string.new_email_message_from_contact,
                title,
                subject
            )
        }

        val emailAppName = AppNameHelper(context).getAppDisplayName(notification.packageName)
        return context.getString(
            R.string.new_email_notification_from_app,
            emailAppName
        )
    }

    override fun shouldSpeakify(): Boolean {
        val peopleList = NotificationExtractionUtils.ExtractPersonList(notification)

        val personListToString = peopleList?.map { "Person{ name: ${it.name}, uri: ${it.uri}, key: ${it.key} }" }

        doLog("PERSON LIST: ${personListToString}")

        saveToRecentContacts()

        val notificationType = getNotificationType()

        if (notificationType in listOf(NotificationTypes.Other, NotificationTypes.Sent))
            return false

        if (notificationType == NotificationTypes.MultipleNewMessages)
            return shouldSpeakifyBasedOnSettings()

        if (notificationType == NotificationTypes.NewMessage)
            return shouldSpeakifyBasedOnSettings() &&
                    super.shouldSpeakify() || appSettingsModel!!.notificationSources.any { source ->
                source.name.equals(title, ignoreCase = true) ||
                peopleList?.any { p ->
                    source.name.equals(p.name?.toString(), ignoreCase = true) ||
                            (p.uri != null && p.uri!!.contains(source.value, ignoreCase = true))
                } == true
            }

        return super.shouldSpeakify()
    }

    private fun saveToRecentContacts() {
        val peopleList = NotificationExtractionUtils.ExtractPersonList(notification)
        val senderPerson = peopleList?.firstOrNull()

        var email = ""
        var name = ""

        if (senderPerson != null) {
            name = senderPerson.name?.toString() ?: ""
            val uri = senderPerson.uri
            if (!uri.isNullOrBlank() && uri.startsWith("mailto:")) {
                email = uri.substringAfter("mailto:").trim()
            }
        }

        if (email.isBlank() && title.contains("@")) {
            email = title.trim()
        }

        if (name.isBlank() && title.isNotBlank()) {
            name = title.trim()
        }

        val key = if (email.isNotBlank()) email else name
        if (key.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DbProvider.GetDb(context)
                val dao = db.recentEmailContactDao()
                dao.insertContact(RecentEmailContactModel(key, email, name))
                dao.pruneOldContacts()
            } catch (e: Exception) {
                LogUtils.LogNonFatalError(TAG, "Error saving email recent contact: ${e.message}", e)
            }
        }
    }

    override fun shouldSpeakifyBasedOnSettings(): Boolean {
        if (!super.shouldSpeakifyBasedOnSettings())
            return false

        if (getNotificationType() == NotificationTypes.MultipleNewMessages)
            return isAnnounceMultipleMessagesEnabled

        if (messageKeywords.isNotEmpty()) {
            val text = NotificationExtractionUtils.ExtractText(notification)

            val possibleMessageExtras = listOf(text,
                NotificationExtractionUtils.ExtractStringExtra(Notification.EXTRA_BIG_TEXT, notification),
            )

            if (!SearchUtils.HasAnyMatchesOf(possibleMessageExtras, messageKeywords))
                return false
        }

        return true
    }
}
