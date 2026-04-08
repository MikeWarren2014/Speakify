package com.mikewarren.speakify.viewsAndViewModels.pages.auth

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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.uiStates.OnboardingUiState

@Composable
fun OnboardingView(
    step: OnboardingUiState,
    viewModel: MainViewModel = hiltViewModel()
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (step) {
            OnboardingUiState.NotStarted,
            OnboardingUiState.SatisfactionSurvey -> SatisfactionSurvey(
                onResult = { result ->
                    viewModel.saveSurveyResult(result)
                    viewModel.updateOnboardingStep(OnboardingUiState.PreferenceGathering)
                }
            )
            OnboardingUiState.PreferenceGathering -> PreferenceGathering(
                onComplete = {
                    viewModel.updateOnboardingStep(OnboardingUiState.ConversionReady)
                }
            )
            OnboardingUiState.ConversionReady -> ConversionReady(
                onSignUp = { viewModel.startTrialConversion() },
                onLater = { viewModel.proceedToTrialSession() }
            )
            OnboardingUiState.Completed -> { /* Should not be reached */ }
        }
    }
}

@Composable
fun SatisfactionSurvey(onResult: (String) -> Unit) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            onClick = { selectedOption?.let { onResult(options[it].first) } },
            enabled = selectedOption != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next))
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
fun PreferenceGathering(onComplete: () -> Unit) {
    // Placeholder for Preference Gathering
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.what_is_your_primary_goal),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        val goals = listOf(
            "Accessibility" to stringResource(R.string.preference_gathering_option_accessibility),
            "Productivity" to stringResource(R.string.preference_gathering_option_productivity),
            "Hands-free Use" to stringResource(R.string.preference_gathering_option_hands_free_use),
            "Other" to stringResource(R.string.preference_gathering_option_other),
        )
        var selectedValue by remember { mutableStateOf(goals[0].first) }

        Column(Modifier.selectableGroup()) {
            goals.forEach { (value, text) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = value == selectedValue,
                            onClick = { selectedValue = value },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (value == selectedValue),
                        onClick = null // null recommended for accessibility with screen readers
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next))
        }
    }
}

@Composable
fun ConversionReady(onSignUp: () -> Unit, onLater: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.conversion_ready_header),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.conversion_ready_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.conversion_ready_button_sign_up))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLater,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
        ) {
            Text(stringResource(R.string.conversion_ready_button_continue))
        }
    }
}
