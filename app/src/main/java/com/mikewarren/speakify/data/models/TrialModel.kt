package com.mikewarren.speakify.data.models

import kotlinx.serialization.Serializable

@Serializable
data class TrialModel(val startTimestamp: Long = 0L,
    val isConverted: Boolean = false)
