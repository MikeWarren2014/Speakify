package com.mikewarren.speakify.data.events

sealed class PhonePermissionEvent: Emittable<Any> {
    object PermissionGranted: PhonePermissionEvent()
    object PermissionDenied : PhonePermissionEvent()
    class Failure(val message: String): PhonePermissionEvent()

    object RequestPermission : PhonePermissionEvent()
}