package com.mikewarren.speakify.strategies.shippingApps

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.strategies.BaseNotificationStrategy
import com.mikewarren.speakify.utils.NotificationExtractionUtils

abstract class BaseShippingAppNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,

): BaseNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    val title = NotificationExtractionUtils.ExtractTitle(notification)
    val text = NotificationExtractionUtils.ExtractText(notification)

    enum class NotificationTypes {
        Shipped,
        OutForDelivery,
        DeliveryUpdate,
        Delivered,
        Other
    }

    val shippingNotificationType = getNotificationType()

    abstract fun getNotificationType(): NotificationTypes

    override fun shouldSpeakify(): Boolean {
        return shippingNotificationType in listOf(NotificationTypes.OutForDelivery,
            NotificationTypes.DeliveryUpdate,
            NotificationTypes.Delivered)
    }

    override fun textToSpeakify(): String {
        if (shippingNotificationType == NotificationTypes.OutForDelivery)
            return context.getString(R.string.shipping_app_package_out_for_delivery,
                getShippingCompany())

        if (shippingNotificationType == NotificationTypes.Delivered)
            return context.getString(R.string.shipping_app_package_delivered,
                getShippingCompany())

        return ""
    }

    abstract fun getShippingCompany(): String
}