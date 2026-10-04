package com.mikewarren.speakify.data.events

import com.mikewarren.speakify.data.EmailContactModel

sealed class EmailContactEvent : Emittable<EmailContactModel> {
    class DataFetched(val data: List<EmailContactModel>) : EmailContactEvent()
    class FetchFailed(val message: String) : EmailContactEvent()
    object RequestData : EmailContactEvent()
}
