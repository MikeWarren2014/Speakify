package com.mikewarren.speakify.data.events

import com.mikewarren.speakify.data.ContactModel

sealed class ContactEvent {
    data class ContactsFetched(val contacts: List<ContactModel>) : ContactEvent()
    object PermissionDenied : ContactEvent()
    data class FetchFailed(val message: String) : ContactEvent()

    object RequestContacts : ContactEvent()
}