package com.mikewarren.speakify.data.events

import com.mikewarren.speakify.data.MessengerContactModel

sealed class MessengerContactEvent : Emittable<MessengerContactModel> {
    class DataFetched(val data: List<MessengerContactModel>) : MessengerContactEvent()
    class FetchFailed(val message: String) : MessengerContactEvent()
    object RequestData : MessengerContactEvent()
}
