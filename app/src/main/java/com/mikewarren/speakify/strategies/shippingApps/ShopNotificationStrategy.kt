package com.mikewarren.speakify.strategies.shippingApps

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.strategies.IsPackageName

@IsPackageName("com.shopify.arrive")
class ShopNotificationStrategy(
    notification: StatusBarNotification,
    appSettingsModel: AppSettingsModel?,
    context: Context,
    ttsManager: TTSManager,
): BaseShippingAppNotificationStrategy(notification, appSettingsModel, context, ttsManager) {
    override fun getNotificationType(): NotificationTypes {
        if (context.getString(R.string.shop_package_delivered) in title)
            return NotificationTypes.Delivered

        if (title.contains(context.getString(R.string.amazon_shopping_out_for_delivery), ignoreCase = true))
            return NotificationTypes.OutForDelivery

        return NotificationTypes.Other
    }

    override fun getShippingCompany(): String {
        return "Shop"
    }
}