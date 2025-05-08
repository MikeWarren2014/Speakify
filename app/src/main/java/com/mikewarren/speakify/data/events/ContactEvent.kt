package com.mikewarren.speakify.data.events

import com.mikewarren.speakify.data.ContactModel

sealed class ContactEvent: Emittable<ContactModel> {
    class DataFetched(val data: List<ContactModel>) : ContactEvent()
    object PermissionDenied : ContactEvent()
    class FetchFailed(val message: String) : ContactEvent()

    object RequestData : ContactEvent()
}