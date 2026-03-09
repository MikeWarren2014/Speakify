package com.mikewarren.speakify.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessengerContactModel(
    val name: String,
    val profilePicUri: String? = null
) : Parcelable {
    constructor() : this("")
}
