package com.mikewarren.speakify.data.events

class PhonePermissionEventBus: BaseEventBus<PhonePermissionEvent>() {
    companion object {
        private var _instance: PhonePermissionEventBus? = null
        fun GetInstance() : PhonePermissionEventBus {
            if (_instance == null) {
                _instance = PhonePermissionEventBus()
            }

            return _instance!!
        }
    }
}