package com.mikewarren.speakify.strategies

import android.app.Notification
import androidx.core.app.Person
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.constants.appSettingsKeys.EmailAppKeys
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.AppNameHelper
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import com.mikewarren.speakify.utils.SearchUtils

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
        TODO("find a way to reliably get the Person data, so that we can construct a Person to return here")
    }

    override fun textToSpeakify(): String {
        val emailAppName = AppNameHelper(context).getAppDisplayName(notification.packageName)
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun shouldSpeakify(): Boolean {
        val personListToString = notification.notification
            .extras
            .getParcelableArray(Notification.EXTRA_PEOPLE_LIST, android.app.Person::class.java)
            ?.map { "Person{ name: ${it.name}, uri: ${it.uri} }" }


        doLog("PERSON LIST: ${personListToString}")

        val notificationType = getNotificationType()
        if (notificationType in listOf(NotificationTypes.Other, NotificationTypes.Sent))
            return false

        if (notificationType == NotificationTypes.MultipleNewMessages)
            return shouldSpeakifyBasedOnSettings()

        if (notificationType == NotificationTypes.NewMessage)
            return shouldSpeakifyBasedOnSettings() && appSettingsModel!!.notificationSources.any { it.value == title }

        return (super.shouldSpeakify())
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
