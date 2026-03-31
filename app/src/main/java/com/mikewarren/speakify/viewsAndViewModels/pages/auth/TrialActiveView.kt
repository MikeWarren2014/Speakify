package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.TrialStatus
import com.mikewarren.speakify.viewsAndViewModels.widgets.EndTrialAlertDialog

@Composable
fun TrialActiveView(
    viewModel: TrialActiveViewModel = hiltViewModel()
) {
    val trialStatus by viewModel.trialStatus.collectAsStateWithLifecycle()
    var showEndTrialConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.trial_active_header),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val daysRemaining = (trialStatus as? TrialStatus.Active)?.daysRemaining ?: Constants.TrialNumberOfDays
            Text(
                text = stringResource(
                    R.string.trial_active_subtext,
                    Constants.TrialNumberOfDays
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            
            Text(
                text = "Days remaining: $daysRemaining",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            FeatureItem(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.trial_feature_scheduling_title),
                description = stringResource(R.string.trial_feature_scheduling_desc)
            )

            FeatureItem(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = stringResource(R.string.trial_feature_messages_title),
                description = stringResource(R.string.trial_feature_messages_desc)
            )

            FeatureItem(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.trial_feature_appointments_title),
                description = stringResource(R.string.trial_feature_appointments_desc)
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.proceedToTrialSession() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.trial_button_enter_app))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showEndTrialConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.trial_button_end_trial))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEndTrialConfirm) {
        EndTrialAlertDialog(
            onDismissRequest = { showEndTrialConfirm = false },
            onConfirm = {
                viewModel.endTrial()
                showEndTrialConfirm = false
            }
        )
    }
}

@Composable
fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
