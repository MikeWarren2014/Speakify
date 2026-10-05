package com.mikewarren.speakify.viewsAndViewModels.pages.brokenNotification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.NotificationAuditRepository
import com.mikewarren.speakify.data.db.NotificationAuditLogModel
import com.mikewarren.speakify.data.db.firestore.BrokenNotificationReportRepository
import com.mikewarren.speakify.data.models.BrokenNotificationReportModel
import com.mikewarren.speakify.data.uiStates.BrokenNotificationReportUiState
import com.mikewarren.speakify.utils.AppInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrokenNotificationReportViewModel @Inject constructor(
    private val auditRepository: NotificationAuditRepository,
    private val reportRepository: BrokenNotificationReportRepository,
    private val appInfoProvider: AppInfoProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrokenNotificationReportUiState())
    val uiState: StateFlow<BrokenNotificationReportUiState> = _uiState.asStateFlow()

    init {
        loadRecentLogs()
    }

    fun loadRecentLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            auditRepository.getRecentLogs(30 * 60 * 1000L).collect { logs ->
                _uiState.update { state ->
                    val selected = state.selectedLog ?: logs.firstOrNull()
                    state.copy(
                        recentLogs = logs,
                        selectedLog = selected,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < 3) state.copy(currentStep = state.currentStep + 1) else state
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 1) state.copy(currentStep = state.currentStep - 1) else state
        }
    }

    fun selectLog(log: NotificationAuditLogModel) {
        _uiState.update {
            it.copy(
                selectedLog = log,
                selectedWordIndices = emptySet()
            )
        }
    }

    fun selectProblem(problem: String) {
        _uiState.update { it.copy(selectedProblem = problem) }
    }

    fun toggleWordIndex(index: Int) {
        _uiState.update { state ->
            val updatedIndices = if (state.selectedWordIndices.contains(index)) {
                state.selectedWordIndices - index
            } else {
                state.selectedWordIndices + index
            }
            state.copy(selectedWordIndices = updatedIndices)
        }
    }

    fun updateUserNotes(notes: String) {
        _uiState.update { it.copy(userNotes = notes) }
    }

    fun submitReport() {
        val currentState = _uiState.value
        val log = currentState.selectedLog ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val rawContent = listOfNotNull(log.rawTitle, log.rawText).joinToString(" ")
            val words = rawContent.split("\\s+".toRegex()).filter { it.isNotBlank() }

            val highlightedText = if (currentState.selectedWordIndices.isNotEmpty()) {
                words.filterIndexed { index, _ -> currentState.selectedWordIndices.contains(index) }
                    .joinToString(" ")
            } else {
                null
            }

            val appVersion = appInfoProvider.getAboutInfo().version

            val report = BrokenNotificationReportModel(
                packageName = log.packageName,
                appDisplayName = log.appDisplayName,
                rawTitle = log.rawTitle,
                rawText = log.rawText,
                speakifiedText = log.speakifiedText,
                silenceReason = log.silenceReason,
                actions = log.actions,
                extras = log.extras,
                selectedProblem = currentState.selectedProblem,
                highlightedIdealText = highlightedText,
                userNotes = currentState.userNotes.ifBlank { null },
                timestamp = System.currentTimeMillis(),
                appVersion = appVersion
            )

            val result = reportRepository.submitReport(report)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isSubmittedSuccessfully = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit report"
                    )
                }
            }
        }
    }
}
