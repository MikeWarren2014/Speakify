package com.mikewarren.speakify.data.events

class EmailContactEventBus private constructor() : BaseEventBus<EmailContactEvent>() {

    companion object {
        private var _instance: EmailContactEventBus? = null
        fun GetInstance() : EmailContactEventBus {
            if (_instance == null) {
                _instance = EmailContactEventBus()
            }

            return _instance!!
        }
    }
}
