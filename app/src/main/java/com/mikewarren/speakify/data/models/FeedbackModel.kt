package com.mikewarren.speakify.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackModel(
    val surveyResult: String? = null,
    val action: String? = null
)
