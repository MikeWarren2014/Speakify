package com.mikewarren.speakify.viewsAndViewModels.pages.brokenNotification

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.db.NotificationAuditLogModel
import com.mikewarren.speakify.data.uiStates.BrokenNotificationReportUiState

val problemOptionLabelsDict = mapOf(
    "IT_DID_NOT_SPEAK" to R.string.problem_label_it_did_not_speak,
    "SHOULD_HAVE_BEEN_SILENT" to R.string.problem_label_should_have_been_silent,
    "TOO_WORDY" to R.string.problem_label_too_wordy,
    "MISSED_IMPORTANT_PART" to R.string.problem_label_missed_important_part,
    "MISPRONOUNCED" to R.string.problem_label_mispronounced,
    "OTHER" to R.string.problem_label_other,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrokenNotificationReportView(
    viewModel: BrokenNotificationReportViewModel,
    onDismiss: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isSubmittedSuccessfully) {
            ReportSuccessView(onDismiss = onDismiss)
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar with optional Back Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.currentStep > 1) {
                    IconButton(onClick = { viewModel.previousStep() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.previous_step)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.broken_notification_report_header),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            PrivacyInfoCard()
            Spacer(modifier = Modifier.height(16.dp))

            // Body Content based on Step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentStep) {
                    1 -> Step1SelectNotification(uiState = uiState, viewModel = viewModel)
                    2 -> Step2SelectProblem(uiState = uiState, viewModel = viewModel)
                    3 -> Step3HighlightAndNotes(uiState = uiState, viewModel = viewModel)
                }
            }

            // Bottom Navigation / Action Button
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState.currentStep) {
                1 -> Button(
                    onClick = { viewModel.nextStep() },
                    enabled = uiState.selectedLog != null && uiState.recentLogs.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.next_step))
                }

                2 -> Button(
                    onClick = { viewModel.nextStep() },
                    enabled = uiState.selectedProblem.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.next_step))
                }

                3 -> Button(
                    onClick = { viewModel.submitReport() },
                    enabled = !uiState.isSubmitting && uiState.selectedLog != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.broken_notification_report_submit))
                    }
                }
            }
        }
    }
}

@Composable
fun Step1SelectNotification(
    uiState: BrokenNotificationReportUiState,
    viewModel: BrokenNotificationReportViewModel
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (uiState.recentLogs.isEmpty()) {
        EmptyLogsCard()
    } else {
        Column {
            Text(
                text = stringResource(R.string.broken_notification_report_step_1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recentLogs) { log ->
                    NotificationLogCard(
                        log = log,
                        isSelected = uiState.selectedLog?.id == log.id,
                        onSelect = { viewModel.selectLog(log) }
                    )
                }
            }
        }
    }
}

@Composable
fun Step2SelectProblem(
    uiState: BrokenNotificationReportUiState,
    viewModel: BrokenNotificationReportViewModel
) {
    Column {
        Text(
            text = stringResource(R.string.broken_notification_report_step_2),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            problemOptionLabelsDict.forEach { (key, labelResourceId) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectProblem(key) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.selectedProblem == key,
                        onClick = { viewModel.selectProblem(key) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(labelResourceId), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3HighlightAndNotes(
    uiState: BrokenNotificationReportUiState,
    viewModel: BrokenNotificationReportViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            uiState.selectedLog?.let { log ->
                val rawContent = listOfNotNull(log.rawTitle, log.rawText).joinToString(" ")
                val words = rawContent.split("\\s+".toRegex()).filter { it.isNotBlank() }

                if (words.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.broken_notification_report_step_3),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        words.forEachIndexed { index, word ->
                            val isSelected = uiState.selectedWordIndices.contains(index)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleWordIndex(index) },
                                label = { Text(word) }
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.userNotes,
                onValueChange = { viewModel.updateUserNotes(it) },
                label = { Text(stringResource(R.string.broken_notification_report_additional_notes)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        uiState.errorMessage?.let { error ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PrivacyInfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = stringResource(R.string.broken_notification_report_privacy_guard),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.broken_notification_report_privacy_guard_description),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun EmptyLogsCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.broken_notification_report_empty_logs_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.broken_notification_report_empty_logs_header_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationLogCard(
    log: NotificationAuditLogModel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(log.packageName) {
        try {
            context.packageManager.getApplicationIcon(log.packageName)
        } catch (_: Exception) {
            null
        }
    }

    val isSpoken = log.silenceReason == null
    val timeAgo = remember(log.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            log.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    OutlinedCard(
        modifier = Modifier
            .width(260.dp)
            .clickable { onSelect() },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.appDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSpoken) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ) {
                Text(
                    text = if (isSpoken) stringResource(R.string.notification_log_card_spoke)
                    else stringResource(R.string.notification_log_card_silenced, log.silenceReason ?: stringResource(R.string.notification_log_card_silence_reason_filtered)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSpoken) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayContent = log.speakifiedText ?: log.rawText ?: log.rawTitle ?: stringResource(R.string.notification_log_card_empty_text)
            Text(
                text = displayContent,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ReportSuccessView(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.report_success_view_icon_description),
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.report_success_view_header),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.report_success_view_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDismiss) {
            Text(stringResource(R.string.report_success_view_button))
        }
    }
}
