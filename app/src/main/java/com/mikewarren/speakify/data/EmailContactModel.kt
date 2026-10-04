package com.mikewarren.speakify.data

import kotlinx.serialization.Serializable

@Serializable
data class EmailContactModel(
    val key: String,
    val email: String = "",
    val name: String = ""
) {
}
