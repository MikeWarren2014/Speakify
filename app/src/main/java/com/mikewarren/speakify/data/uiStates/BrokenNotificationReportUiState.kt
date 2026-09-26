package com.mikewarren.speakify.data.uiStates

import com.mikewarren.speakify.data.db.NotificationAuditLogModel

data class BrokenNotificationReportUiState(
    val recentLogs: List<NotificationAuditLogModel> = emptyList(),
    val selectedLog: NotificationAuditLogModel? = null,
    val selectedProblem: String = "IT_DID_NOT_SPEAK",
    val selectedWordIndices: Set<Int> = emptySet(),
    val userNotes: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isSubmittedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)
