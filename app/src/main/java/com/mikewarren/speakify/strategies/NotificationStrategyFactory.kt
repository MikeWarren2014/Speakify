package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.services.TTSManager
import kotlin.reflect.full.primaryConstructor

object NotificationStrategyFactory {
    fun CreateFrom(notification: StatusBarNotification,
                   appSettingsModel: AppSettingsModel?,
                   context: Context,
                   ttsManager: TTSManager,
    ) : BaseNotificationStrategy {
        val strategyClass = NotificationStrategyRegistry.findStrategyClass(notification)
        
        return strategyClass.primaryConstructor?.call(
            notification,
            appSettingsModel,
            context,
            ttsManager
        ) ?: SimpleNotificationStrategy(notification, appSettingsModel, context, ttsManager)
    }
}
