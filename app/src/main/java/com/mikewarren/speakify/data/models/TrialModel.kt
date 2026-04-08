package com.mikewarren.speakify.data.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.mikewarren.speakify.data.TrialStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class TrialModel(
    val startTimestamp: Long = 0L,
    @get:Exclude
    @set:Exclude
    var status: TrialStatus = TrialStatus.NotStarted,
){
    @get:PropertyName("status")
    @set:PropertyName("status")
    var statusString: String?
        get() = status.statusText.lowercase()
        set(value) {
            status = TrialStatus.valueOf(value ?: "")
        }
}
