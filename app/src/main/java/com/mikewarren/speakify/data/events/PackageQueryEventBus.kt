package com.mikewarren.speakify.data.events

class PackageQueryEventBus : BaseEventBus<PackageQueryEvent>() {
    companion object {
        private var _instance: PackageQueryEventBus? = null
        fun GetInstance() : PackageQueryEventBus {
            if (_instance == null) {
                _instance = PackageQueryEventBus()
            }

            return _instance!!
        }
    }
}
