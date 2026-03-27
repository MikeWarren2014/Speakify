package com.mikewarren.speakify.strategies

import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.constants.appSettingsKeys.MessagingAppKeys

/**
 * Isolates the logic for parsing messaging-specific settings from the generic AppSettingsModel.
 */
interface IMessageSettingsParser {
    val appSettingsModel: AppSettingsModel?

    val isReadMessagesEnabled: Boolean
        get() = appSettingsModel?.getBooleanSetting(MessagingAppKeys.KEY_READ_MESSAGES) == true

    val isIgnoreSingleWordMessagesEnabled: Boolean
        get() = appSettingsModel?.getBooleanSetting(MessagingAppKeys.KEY_IGNORE_SINGLE_WORD_MESSAGES) == true

    val isIgnoreReactionsEnabled: Boolean
        get() = appSettingsModel?.getBooleanSetting(MessagingAppKeys.KEY_IGNORE_REACTIONS) == true
}
