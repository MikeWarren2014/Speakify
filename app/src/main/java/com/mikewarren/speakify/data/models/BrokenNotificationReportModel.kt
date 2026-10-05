package com.mikewarren.speakify.data.models

import com.mikewarren.speakify.data.serializers.TimestampSerializer
import kotlinx.serialization.Serializable

@Serializable
data class BrokenNotificationReportModel(
    val packageName: String = "",
    val appDisplayName: String = "",
    val rawTitle: String? = null,
    val rawText: String? = null,
    val speakifiedText: String? = null,
    val silenceReason: String? = null,
    val actions: String? = null,
    val extras: String? = null,
    val selectedProblem: String = "",
    val highlightedIdealText: String? = null,
    val userNotes: String? = null,
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String? = null,
    val userId: String? = null
)
