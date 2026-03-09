package com.mikewarren.speakify.data.events

class MessengerContactEventBus private constructor() : BaseEventBus<MessengerContactEvent>() {

    companion object {
        private var _instance: MessengerContactEventBus? = null
        fun GetInstance() : MessengerContactEventBus {
            if (_instance == null) {
                _instance = MessengerContactEventBus()
            }

            return _instance!!
        }
    }
}
