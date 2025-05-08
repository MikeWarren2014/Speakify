package com.mikewarren.speakify.data.events

class ContactEventBus : BaseEventBus<ContactEvent>() {

    companion object {
        private var _instance: ContactEventBus? = null
        fun GetInstance() : ContactEventBus {
            if (_instance == null) {
                _instance = ContactEventBus()
            }

            return _instance!!
        }
    }
}