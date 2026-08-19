package com.mikewarren.speakify.strategies.shippingApps

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager

class FedExNotificationStrategy(notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,
): BaseShippingAppNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    override fun getNotificationType(): NotificationTypes {
        if (title == context.getString(R.string.fedEx_out_for_delivery))
            return NotificationTypes.OutForDelivery
        if (title == context.getString(R.string.fedEx_delivery_rescheduled))
            return NotificationTypes.DeliveryUpdate
        if (title == context.getString(R.string.package_delivered))
            return NotificationTypes.Delivered

        return NotificationTypes.Other
    }

    override fun getShippingCompany(): String {
        return "FedEx"
    }

    override fun textToSpeakify(): String {
        val baseAnnouncementText = super.textToSpeakify()

        if ((baseAnnouncementText.isNotEmpty()) || (shippingNotificationType != NotificationTypes.DeliveryUpdate))
            return baseAnnouncementText

        return text.replace("""\s?\d{5,}\s?""".toRegex(), "")
    }

}