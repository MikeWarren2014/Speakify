package com.mikewarren.speakify.data.events

sealed class NotificationPermissionEvent {
    object PermissionGranted : NotificationPermissionEvent()
    object PermissionDenied : NotificationPermissionEvent()
    data class Failure(val message: String) : NotificationPermissionEvent()
}