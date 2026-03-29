package com.mikewarren.speakify.data.models.scheduling

import kotlinx.serialization.Serializable

@Serializable
sealed class StatusModel(val isAppOn: Boolean) {
    @Serializable
    data object On : StatusModel(true)
    
    @Serializable
    data class Off(val turnOnTime: Long? = null) : StatusModel(false)
}
