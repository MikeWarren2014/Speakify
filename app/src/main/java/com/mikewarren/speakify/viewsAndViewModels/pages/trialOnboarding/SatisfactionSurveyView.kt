package com.mikewarren.speakify.viewsAndViewModels.pages.trialOnboarding

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
import androidx.core.net.toUri
import com.mikewarren.speakify.data.Constants

private enum class SurveyStep {
    SENTIMENT,
    RATE_INVITE,
    FEEDBACK_INVITE
}

@Composable
fun SatisfactionSurvey(onResult: (String) -> Unit) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var currentStep by remember { mutableStateOf(SurveyStep.SENTIMENT) }
    val context = LocalContext.current

    val options = listOf(
        Triple("Very Dissatisfied",
            stringResource(R.string.satisfaction_survey_very_dissatisfied_header),
            Icons.Default.SentimentVeryDissatisfied),
        Triple("Dissatisfied",
            stringResource(R.string.satisfaction_survey_dissatisfied_header),
            Icons.Default.SentimentDissatisfied),
        Triple("Neutral",
            stringResource(R.string.satisfaction_survey_neutral_header),
            Icons.Default.SentimentNeutral),
        Triple("Satisfied",
            stringResource(R.string.satisfaction_survey_satisfied_header),
            Icons.Default.SentimentSatisfied),
        Triple("Very Satisfied",
            stringResource(R.string.satisfaction_survey_very_satisfied_header),
            Icons.Default.SentimentVerySatisfied),
    )

    Crossfade(targetState = currentStep, label = "SurveyStepAnimation") { step ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                SurveyStep.SENTIMENT -> {
                    Text(
                        text = stringResource(R.string.satisfaction_survey_header),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        options.forEachIndexed { index, (_, label, icon) ->
                            SurveyIcon(
                                icon = icon,
                                label = label,
                                isSelected = selectedOption == index,
                                onClick = { selectedOption = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            selectedOption?.let {
                                val result = options[it].first
                                // Branch based on sentiment index
                                when (it) {
                                    0, 1 -> currentStep = SurveyStep.FEEDBACK_INVITE
                                    3, 4 -> currentStep = SurveyStep.RATE_INVITE
                                    else -> onResult(result) // Neutral just proceeds
                                }
                            }
                        },
                        enabled = selectedOption != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.next))
                    }
                }

                SurveyStep.RATE_INVITE -> {
                    SurveyFollowUp(
                        title = stringResource(R.string.survey_rate_invite_title),
                        description = stringResource(R.string.survey_rate_invite_desc),
                        primaryButtonText = stringResource(R.string.survey_rate_invite_btn_text),
                        onPrimaryClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                "market://details?id=${context.packageName}".toUri())
                            context.startActivity(intent)
                            onResult("Rated")
                        },
                        onSecondaryClick = { onResult("Rate Later") }
                    )
                }

                SurveyStep.FEEDBACK_INVITE -> {
                    val feedbackSubject = stringResource(R.string.feedback_email_subject)
                    
                    SurveyFollowUp(
                        title = stringResource(R.string.feedback_invite_title),
                        description = stringResource(R.string.feedback_invite_desc),
                        primaryButtonText = stringResource(R.string.feedback_invite_btn_text),
                        onPrimaryClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:${Constants.SupportEmail}".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                            }
                            context.startActivity(intent)
                            onResult("Feedback Sent")
                        },
                        onSecondaryClick = { onResult("Feedback Declined") }
                    )
                }
            }
        }
    }
}

@Composable
fun SurveyIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.selectable(
            selected = isSelected,
            onClick = onClick,
            role = Role.RadioButton
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(48.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SurveyFollowUp(
    title: String,
    description: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onPrimaryClick, modifier = Modifier.fillMaxWidth()) {
        Text(primaryButtonText)
    }
    TextButton(onClick = onSecondaryClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.survey_follow_up_maybe_later))
    }
}
