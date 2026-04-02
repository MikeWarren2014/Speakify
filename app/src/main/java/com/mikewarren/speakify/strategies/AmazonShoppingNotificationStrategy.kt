package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils

class AmazonShoppingNotificationStrategy(notification: StatusBarNotification,
                                         appSettingsModel: AppSettingsModel?,
                                         context: Context,
    ttsManager: TTSManager) : BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    val title = NotificationExtractionUtils.ExtractTitle(notification)
    val text = NotificationExtractionUtils.ExtractText(notification)

    override fun shouldSpeakify(): Boolean {
        return title.contains(context.getString(R.string.amazon_shopping_your_package_keyword),
            ignoreCase = true)
    }

    override fun textToSpeakify(): String {
        // Extract any sequence of digits from the text. 
        // In the context of Amazon notifications like "X stops away", the first number is usually the count.
        val regex = """(\d+)""".toRegex()
        val matchResult = regex.find(text)

        return if (matchResult != null) {
            val stops = matchResult.groupValues[1]
            context.getString(R.string.amazon_shopping_strategy_stops_away, stops)
        } else {
            context.getString(R.string.amazon_shopping_strategy_on_its_way)
        }
    }
}
