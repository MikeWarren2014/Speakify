package com.mikewarren.speakify.data.models

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class TrialModel(
    val startTimestamp: Long = 0L,
    @get:PropertyName("isConverted")
    @PropertyName("isConverted")
    val isConverted: Boolean = false
)
