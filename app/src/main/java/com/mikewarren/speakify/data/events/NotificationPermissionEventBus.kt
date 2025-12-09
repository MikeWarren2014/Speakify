package com.mikewarren.speakify.data.events

class NotificationPermissionEventBus private constructor() :
    BaseEventBus<NotificationPermissionEvent>() {
    companion object {
        @Volatile
        private var instance: NotificationPermissionEventBus? = null

        fun GetInstance(): NotificationPermissionEventBus {
            return instance ?: synchronized(this) {
                instance ?: NotificationPermissionEventBus().also { instance = it }
            }
        }
    }
}