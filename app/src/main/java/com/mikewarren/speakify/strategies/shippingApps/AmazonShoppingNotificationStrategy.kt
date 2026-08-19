package com.mikewarren.speakify.strategies.shippingApps

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.TimeUtils

class AmazonShoppingNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,
): BaseShippingAppNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    override fun getNotificationType(): NotificationTypes {

        if (title in listOf(context.getString(R.string.amazon_shopping_out_for_delivery),
            context.getString(R.string.amazon_shopping_see_where_your_delivery_is)))
            return NotificationTypes.OutForDelivery

        if (title == context.getString(R.string.package_delivered))
            return NotificationTypes.Delivered

        return NotificationTypes.Other
    }

    override fun textToSpeakify(): String {
        val baseAnnouncementText = super.textToSpeakify()

        if (shippingNotificationType == NotificationTypes.OutForDelivery) {
            if (title == context.getString(R.string.amazon_shopping_see_where_your_delivery_is))
                return "$baseAnnouncementText $text"

            if (title == context.getString(R.string.amazon_shopping_out_for_delivery))
                return "$baseAnnouncementText ${getRelativeTimeRangeString()}"
        }

        return baseAnnouncementText
    }

    internal fun getRelativeTimeRangeString(): String {
        if (shippingNotificationType != NotificationTypes.OutForDelivery)
            throw Exception("getRelativeTimeRangeString() called on non-OutForDelivery notification")

        if (!text.contains(context.getString(R.string.amazon_arrival_today_prefix), ignoreCase = true))
            return ""

        val relativeTimeStrings = TimeUtils.ExtractTimeRange(text)
            .mapNotNull { time ->
                TimeUtils.GetLocalDateTimeFromTimeString(time)?.let {
                    TimeUtils.GetQuantityString(context, it)
                }
            }

        if (relativeTimeStrings.size >= 2) {
            return context.getString(R.string.amazon_delivery_between_and,
                relativeTimeStrings[0],
                relativeTimeStrings[1])
        }

        return ""
    }

    override fun getShippingCompany(): String {
        return "Amazon"
    }
}